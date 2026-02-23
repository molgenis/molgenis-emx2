"""CLI entry point for the EMX2 HPC daemon.

Commands:
    run       — Start the daemon loop
    register  — Register with EMX2 and exit
    check     — Validate config, connectivity, and Slurm command availability
"""

from __future__ import annotations

import json as json_module
import logging
import shutil
import sys

import click

from .config import load_config
from .profiles import derive_capabilities


class JsonFormatter(logging.Formatter):
    """Structured JSON log formatter."""

    def format(self, record):
        log_entry = {
            "timestamp": self.formatTime(record),
            "level": record.levelname,
            "logger": record.name,
            "message": record.getMessage(),
        }
        if record.exc_info and record.exc_info[0]:
            log_entry["exception"] = self.formatException(record.exc_info)
        return json_module.dumps(log_entry)


def _setup_logging(verbose: bool, json_logs: bool) -> None:
    """Configure logging. Idempotent — only applies on first call."""
    if logging.root.handlers:
        # Already configured (e.g. group-level --verbose). Upgrade to DEBUG if requested.
        if verbose:
            logging.root.setLevel(logging.DEBUG)
        return

    level = logging.DEBUG if verbose else logging.INFO
    if json_logs:
        handler = logging.StreamHandler()
        handler.setFormatter(JsonFormatter())
        logging.root.addHandler(handler)
        logging.root.setLevel(level)
    else:
        logging.basicConfig(
            level=level,
            format="%(asctime)s %(levelname)-8s %(name)s — %(message)s",
        )

    # Silence noisy HTTP internals — our client already logs what matters
    for noisy in ("httpcore", "httpx", "hpack", "urllib3"):
        logging.getLogger(noisy).setLevel(logging.WARNING)


# Common options shared by all subcommands so -v works in any position
_verbose_option = click.option(
    "--verbose", "-v", is_flag=True, help="Enable debug logging"
)
_json_logs_option = click.option(
    "--json-logs", is_flag=True, help="Output structured JSON logs"
)


@click.group()
@click.option(
    "--config",
    "-c",
    default="config.yaml",
    help="Path to config file",
    type=click.Path(exists=True),
)
@_verbose_option
@_json_logs_option
@click.pass_context
def main(ctx, config, verbose, json_logs):
    """EMX2 HPC Daemon — outbound execution bridge for HPC clusters."""
    _setup_logging(verbose, json_logs)
    ctx.ensure_object(dict)
    ctx.obj["config"] = load_config(config)


@main.command()
@click.option("--simulate", is_flag=True, help="Simulate Slurm execution")
@_verbose_option
@_json_logs_option
@click.pass_context
def run(ctx, simulate, verbose, json_logs):
    """Start the daemon main loop."""
    _setup_logging(verbose, json_logs)
    from .daemon import HpcDaemon

    config = ctx.obj["config"]
    daemon = HpcDaemon(config, simulate=simulate)
    daemon.run()


@main.command()
@click.option("--simulate", is_flag=True, help="Simulate Slurm execution")
@_verbose_option
@_json_logs_option
@click.pass_context
def once(ctx, simulate, verbose, json_logs):
    """Run a single poll-claim-monitor cycle, then exit."""
    _setup_logging(verbose, json_logs)
    from .daemon import HpcDaemon

    config = ctx.obj["config"]
    daemon = HpcDaemon(config, simulate=simulate)
    daemon.run_once()


@main.command()
@_verbose_option
@_json_logs_option
@click.pass_context
def register(ctx, verbose, json_logs):
    """Register this worker with EMX2 and exit."""
    _setup_logging(verbose, json_logs)
    from .daemon import HpcDaemon

    config = ctx.obj["config"]
    daemon = HpcDaemon(config)
    try:
        daemon._register()
        capabilities = derive_capabilities(config)
        click.echo(f"Registered worker {config.emx2.worker_id}")
        click.echo(f"  Capabilities: {len(capabilities)}")
        for cap in capabilities:
            click.echo(f"    - {cap['processor']}:{cap['profile']}")
    except Exception as e:
        click.echo(f"Registration failed: {e}", err=True)
        sys.exit(1)
    finally:
        daemon.client.close()


@main.command()
@_verbose_option
@_json_logs_option
@click.pass_context
def check(ctx, verbose, json_logs):
    """Validate config, connectivity, and Slurm command availability."""
    _setup_logging(verbose, json_logs)
    config = ctx.obj["config"]
    ok = True

    # Check config
    click.echo("Configuration:")
    click.echo(f"  EMX2 URL: {config.emx2.base_url}")
    click.echo(f"  Worker ID: {config.emx2.worker_id}")
    click.echo(f"  Secret configured: {'yes' if config.emx2.shared_secret else 'no'}")
    click.echo(f"  Profiles: {len(config.profiles)}")
    for key in config.profiles:
        p = config.profiles[key]
        click.echo(
            f"    {key}: {p.sif_image} ({p.partition}, {p.cpus}cpu, {p.memory},"
            f" artifacts={p.artifact_residence})"
        )

    # Check Slurm commands
    click.echo("\nSlurm commands:")
    for cmd in ("sbatch", "squeue", "sacct", "scancel"):
        path = shutil.which(cmd)
        if path:
            click.echo(f"  {cmd}: {path}")
        else:
            click.echo(f"  {cmd}: NOT FOUND", err=True)
            ok = False

    # Check Apptainer
    click.echo("\nApptainer:")
    apptainer_path = shutil.which("apptainer")
    if apptainer_path:
        click.echo(f"  apptainer: {apptainer_path}")
    else:
        click.echo("  apptainer: NOT FOUND", err=True)
        ok = False

    # Check connectivity
    click.echo(f"\nConnectivity to {config.emx2.base_url}:")
    try:
        from .client import HpcClient

        client = HpcClient(
            base_url=config.emx2.base_url,
            worker_id=config.emx2.worker_id,
            shared_secret=config.emx2.shared_secret,
            max_retries=1,
        )
        # Try to list jobs as a connectivity test
        client.poll_pending_jobs()
        click.echo("  Connection: OK")
        client.close()
    except Exception as e:
        click.echo(f"  Connection: FAILED ({e})", err=True)
        ok = False

    if ok:
        click.echo("\nAll checks passed.")
    else:
        click.echo("\nSome checks failed.", err=True)
        sys.exit(1)


if __name__ == "__main__":
    main()
