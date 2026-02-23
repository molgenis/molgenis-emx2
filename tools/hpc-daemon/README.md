# EMX2 HPC Daemon

Outbound execution bridge daemon for offloading compute workloads from MOLGENIS EMX2 to HPC clusters.

## Quick Start

```bash
# Install
uv pip install -e .

# Check config and connectivity
emx2-hpc-daemon -c config.yaml check

# Run the daemon
emx2-hpc-daemon -c config.yaml run
```
