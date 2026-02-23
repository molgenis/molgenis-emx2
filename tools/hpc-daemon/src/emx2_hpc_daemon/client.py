"""HTTP client for the EMX2 HPC API with protocol headers and HATEOAS link following.

Wraps httpx with:
- Required protocol headers (X-EMX2-API-Version, X-Request-Id, X-Timestamp, etc.)
- HMAC-SHA256 authentication
- HATEOAS link following from responses
- Retry with exponential backoff for transient errors
"""

from __future__ import annotations

import logging
import time
import uuid
from dataclasses import dataclass

import httpx

from .auth import build_authorization_header

logger = logging.getLogger(__name__)

API_VERSION = "2025-01"

# Exceptions for specific error conditions


class ClaimConflict(Exception):
    """Raised when a job claim fails due to conflict (409)."""


class TransitionError(Exception):
    """Raised when a job transition is invalid."""


class NotFoundError(Exception):
    """Raised when a resource is not found (404)."""


@dataclass
class HpcClient:
    """Client for the EMX2 HPC bridge API."""

    base_url: str
    worker_id: str
    shared_secret: str
    auth_mode: str = "hmac"
    max_retries: int = 3
    backoff_base: float = 1.0

    def __post_init__(self):
        self._http = httpx.Client(base_url=self.base_url, timeout=30.0)

    def close(self):
        self._http.close()

    def _headers(self, method: str, path: str, body: str = "") -> dict[str, str]:
        """Build all required protocol headers including auth."""
        request_id = str(uuid.uuid4())
        headers = {
            "X-EMX2-API-Version": API_VERSION,
            "X-Request-Id": request_id,
            "X-Worker-Id": self.worker_id,
            "Content-Type": "application/json",
        }

        if self.shared_secret and self.auth_mode == "hmac":
            auth_headers, _, _ = build_authorization_header(
                method, path, body, self.shared_secret
            )
            headers.update(auth_headers)
        elif self.shared_secret and self.auth_mode == "token":
            headers["x-molgenis-token"] = self.shared_secret
            headers["X-Timestamp"] = str(int(time.time()))
        else:
            # No secret configured — send timestamp anyway for protocol compliance
            headers["X-Timestamp"] = str(int(time.time()))

        return headers

    def _request(self, method: str, path: str, json: dict | None = None) -> dict:
        """Make an HTTP request with retries and error handling."""
        import json as json_module

        body = json_module.dumps(json) if json else ""

        for attempt in range(self.max_retries):
            try:
                headers = self._headers(method, path, body)
                response = self._http.request(
                    method,
                    path,
                    content=body if body else None,
                    headers=headers,
                )

                if response.status_code == 404:
                    raise NotFoundError(f"{method} {path}: not found")
                if response.status_code == 409:
                    data = response.json()
                    detail = data.get("detail", "Conflict")
                    if "claim" in path.lower():
                        raise ClaimConflict(detail)
                    raise TransitionError(detail)
                if response.status_code >= 500:
                    # Transient server error — retry
                    if attempt < self.max_retries - 1:
                        wait = self.backoff_base * (2**attempt)
                        logger.warning(
                            "Server error %d on %s %s, retrying in %.1fs",
                            response.status_code,
                            method,
                            path,
                            wait,
                        )
                        time.sleep(wait)
                        continue
                    response.raise_for_status()

                if not (200 <= response.status_code < 300):
                    # Unhandled non-2xx (e.g. 400 Bad Request)
                    response.raise_for_status()

                if response.status_code == 204 or not response.content:
                    return {}
                return response.json()

            except (httpx.ConnectError, httpx.TimeoutException) as e:
                if attempt < self.max_retries - 1:
                    wait = self.backoff_base * (2**attempt)
                    logger.warning(
                        "Connection error on %s %s: %s, retrying in %.1fs",
                        method,
                        path,
                        e,
                        wait,
                    )
                    time.sleep(wait)
                    continue
                raise

        return {}  # unreachable

    # --- High-level API methods ---

    def register_worker(self, hostname: str, capabilities: list[dict]) -> dict:
        """Register this worker with the EMX2 server."""
        return self._request(
            "POST",
            "/api/hpc/workers/register",
            json={
                "worker_id": self.worker_id,
                "hostname": hostname,
                "capabilities": capabilities,
            },
        )

    def poll_pending_jobs(
        self, processor: str | None = None, profile: str | None = None
    ) -> list[dict]:
        """Poll for pending jobs matching capabilities."""
        params = "?status=PENDING"
        if processor:
            params += f"&processor={processor}"
        if profile:
            params += f"&profile={profile}"
        result = self._request("GET", f"/api/hpc/jobs{params}")
        return result.get("items", [])

    def claim_job(self, job_id: str) -> dict:
        """Claim a pending job. Raises ClaimConflict if already claimed."""
        return self._request(
            "POST",
            f"/api/hpc/jobs/{job_id}/claim",
            json={"worker_id": self.worker_id},
        )

    def transition_job(
        self,
        job_id: str,
        status: str,
        detail: str | None = None,
        slurm_job_id: str | None = None,
    ) -> dict:
        """Report a job status transition."""
        body = {
            "status": status,
            "worker_id": self.worker_id,
        }
        if detail:
            body["detail"] = detail
        if slurm_job_id:
            body["slurm_job_id"] = slurm_job_id
        return self._request("POST", f"/api/hpc/jobs/{job_id}/transition", json=body)

    def get_job(self, job_id: str) -> dict:
        """Get a job by ID."""
        return self._request("GET", f"/api/hpc/jobs/{job_id}")

    def cancel_job(self, job_id: str) -> dict:
        """Cancel a job."""
        return self._request("POST", f"/api/hpc/jobs/{job_id}/cancel")

    def follow_link(self, response: dict, rel: str) -> dict:
        """Follow a HATEOAS link from a response."""
        links = response.get("_links", {})
        link = links.get(rel)
        if not link:
            raise KeyError(f"No '{rel}' link in response")
        method = link.get("method", "GET")
        href = link["href"]
        return self._request(method, href)

    def get_artifact(self, artifact_id: str) -> dict:
        """Get artifact metadata."""
        return self._request("GET", f"/api/hpc/artifacts/{artifact_id}")

    def list_artifact_files(self, artifact_id: str) -> list[dict]:
        """List files belonging to an artifact."""
        result = self._request("GET", f"/api/hpc/artifacts/{artifact_id}/files")
        return result.get("items", [])

    def download_artifact_file(self, artifact_id: str, file_id: str, dest_path: str) -> None:
        """Download a single artifact file to a local path.

        For managed artifacts, downloads the file content from the server.
        For external artifacts, the content_url should be used directly.
        """
        from pathlib import Path

        path = f"/api/hpc/artifacts/{artifact_id}/files/{file_id}/content"
        headers = self._headers("GET", path, "")

        response = self._http.request("GET", path, headers=headers)
        if response.status_code == 404:
            raise NotFoundError(f"Artifact file {file_id} not found")
        response.raise_for_status()

        dest = Path(dest_path)
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_bytes(response.content)
        logger.info("Downloaded artifact file %s to %s (%d bytes)", file_id, dest_path, len(response.content))

    def download_artifact_files(self, artifact_id: str, dest_dir: str) -> list[str]:
        """Download all files from an artifact to a directory.

        Returns a list of local file paths that were downloaded.
        """
        from pathlib import Path

        files = self.list_artifact_files(artifact_id)
        downloaded = []
        for f in files:
            file_path = f.get("path", f.get("id", "unknown"))
            local_path = Path(dest_dir) / file_path
            try:
                self.download_artifact_file(artifact_id, f["id"], str(local_path))
                downloaded.append(str(local_path))
            except Exception:
                logger.warning("Failed to download file %s from artifact %s", f.get("id"), artifact_id)
        return downloaded

    def create_artifact(
        self,
        artifact_type: str = "blob",
        fmt: str | None = None,
        residence: str = "managed",
        metadata: dict | None = None,
    ) -> dict:
        """Create a new artifact."""
        body: dict = {"type": artifact_type, "residence": residence}
        if fmt:
            body["format"] = fmt
        if metadata:
            body["metadata"] = metadata
        return self._request("POST", "/api/hpc/artifacts", json=body)

    def upload_artifact_file(
        self,
        artifact_id: str,
        path: str,
        file_content: bytes,
        content_type: str = "application/octet-stream",
        role: str | None = None,
    ) -> dict:
        """Upload a file to an artifact (metadata-only for now; multipart coming)."""
        import hashlib

        sha256 = hashlib.sha256(file_content).hexdigest()
        body = {
            "path": path,
            "sha256": sha256,
            "size_bytes": len(file_content),
            "content_type": content_type,
        }
        if role:
            body["role"] = role
        return self._request("POST", f"/api/hpc/artifacts/{artifact_id}/files", json=body)

    def commit_artifact(self, artifact_id: str, sha256: str, size_bytes: int) -> dict:
        """Commit an artifact with final hash and size."""
        return self._request(
            "POST",
            f"/api/hpc/artifacts/{artifact_id}/commit",
            json={"sha256": sha256, "size_bytes": size_bytes},
        )

    def heartbeat(self) -> dict:
        """Send a heartbeat to the server.

        Uses the dedicated heartbeat endpoint which only updates the
        timestamp, without replacing the worker's capabilities.
        """
        return self._request(
            "POST",
            f"/api/hpc/workers/{self.worker_id}/heartbeat",
        )
