import { request } from "graphql-request";

const GRAPHQL_URL = "/_SYSTEM_/graphql";
const REST_BASE = "/api/hpc";

/** Safely encode a JS string as a GraphQL string literal. */
function gqlString(value) {
  return JSON.stringify(String(value));
}

/**
 * Returns true if the error indicates HPC tables don't exist yet
 * (e.g. FieldUndefined for HpcJobs, HpcWorkers, etc.).
 */
function isSchemaNotReady(err) {
  const msg = err?.response?.errors?.[0]?.message ?? err?.message ?? "";
  return msg.includes("FieldUndefined") || msg.includes("is undefined");
}

/**
 * Check whether HPC is enabled via the health endpoint.
 * Returns { ok, hpc_enabled, database } or null on error.
 */
export async function fetchHpcHealth() {
  try {
    const resp = await fetch(`${REST_BASE}/health`);
    if (!resp.ok) return null;
    return await resp.json();
  } catch {
    return null;
  }
}

/** Common REST headers for HPC API calls. */
function hpcHeaders() {
  return {
    "X-EMX2-API-Version": "2025-01",
    "X-Request-Id": crypto.randomUUID(),
    "X-Timestamp": new Date().toISOString(),
  };
}

/**
 * Fetch jobs from the SYSTEM schema via GraphQL.
 * @param {Object} opts - { status, processor, limit, offset }
 */
export async function fetchJobs({ status, processor, limit = 50, offset = 0 } = {}) {
  const filters = [];
  if (status) {
    filters.push(`status: { name: { equals: ${gqlString(status)} } }`);
  }
  if (processor) {
    filters.push(`processor: { equals: ${gqlString(processor)} }`);
  }
  const filterClause = filters.length
    ? `filter: { ${filters.join(", ")} }`
    : "";
  const aggLine = filterClause
    ? `HpcJobs_agg(${filterClause}) { count }`
    : `HpcJobs_agg { count }`;

  const query = `{
    HpcJobs(
      ${filterClause ? filterClause + "," : ""}
      limit: ${limit},
      offset: ${offset},
      orderby: { created_at: DESC }
    ) {
      id processor profile
      status { name }
      worker_id { worker_id }
      output_artifact_id { id name type status { name } }
      log_artifact_id { id name type status { name } }
      slurm_job_id submit_user
      created_at claimed_at submitted_at started_at completed_at
      parameters inputs
    }
    ${aggLine}
  }`;

  try {
    const data = await request(GRAPHQL_URL, query);
    return {
      items: (data.HpcJobs || []).map(normalizeJob),
      totalCount: data.HpcJobs_agg?.count ?? 0,
    };
  } catch (err) {
    if (isSchemaNotReady(err)) {
      return { items: [], totalCount: 0, schemaNotReady: true };
    }
    throw err;
  }
}

/**
 * Fetch a single job with its transitions.
 * @param {string} jobId
 */
export async function fetchJobDetail(jobId) {
  const query = `{
    HpcJobs(filter: { id: { equals: ${gqlString(jobId)} } }) {
      id processor profile
      status { name }
      worker_id { worker_id }
      output_artifact_id { id name type status { name } }
      log_artifact_id { id name type status { name } }
      slurm_job_id submit_user
      created_at claimed_at submitted_at started_at completed_at
      parameters inputs
    }
    HpcJobTransitions(
      filter: { job_id: { id: { equals: ${gqlString(jobId)} } } }
      orderby: { timestamp: ASC }
    ) {
      id from_status to_status timestamp worker_id detail
    }
  }`;

  try {
    const data = await request(GRAPHQL_URL, query);
    const job = data.HpcJobs?.[0];
    return {
      job: job ? normalizeJob(job) : null,
      transitions: data.HpcJobTransitions || [],
    };
  } catch (err) {
    if (isSchemaNotReady(err)) {
      return { job: null, transitions: [], schemaNotReady: true };
    }
    throw err;
  }
}

/**
 * Submit a new job via the REST API.
 * @param {Object} payload - { processor, profile, parameters, submit_user, inputs }
 */
export async function submitJob(payload) {
  const resp = await fetch(`${REST_BASE}/jobs`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...hpcHeaders(),
    },
    body: JSON.stringify(payload),
  });
  if (!resp.ok) {
    const err = await resp.json().catch(() => ({}));
    throw new Error(err.detail || `HTTP ${resp.status}`);
  }
  return resp.json();
}

/**
 * Cancel an active job via the REST API.
 * @param {string} jobId
 * @returns {Promise<void>}
 */
export async function cancelJob(jobId) {
  const resp = await fetch(`${REST_BASE}/jobs/${jobId}/cancel`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...hpcHeaders(),
    },
    body: JSON.stringify({}),
  });
  if (!resp.ok) {
    const err = await resp.json().catch(() => ({}));
    throw new Error(err.detail || `HTTP ${resp.status}`);
  }
}

/**
 * Delete a terminal job via the REST API.
 * @param {string} jobId
 * @returns {Promise<void>}
 */
export async function deleteJob(jobId) {
  const resp = await fetch(`${REST_BASE}/jobs/${jobId}`, {
    method: "DELETE",
    headers: hpcHeaders(),
  });
  if (!resp.ok) {
    const err = await resp.json().catch(() => ({}));
    throw new Error(err.detail || `HTTP ${resp.status}`);
  }
}

/**
 * Fetch registered workers with their capabilities via GraphQL.
 */
export async function fetchWorkers() {
  const query = `{
    HpcWorkers(orderby: { last_heartbeat_at: DESC }) {
      worker_id hostname registered_at last_heartbeat_at
    }
    HpcWorkerCapabilities {
      worker_id { worker_id }
      processor profile max_concurrent_jobs
    }
  }`;

  try {
    const data = await request(GRAPHQL_URL, query);
    const workers = data.HpcWorkers || [];
    const caps = data.HpcWorkerCapabilities || [];

    return workers.map((w) => ({
      ...w,
      capabilities: caps
        .filter((c) => (c.worker_id?.worker_id ?? c.worker_id) === w.worker_id)
        .map(({ processor, profile, max_concurrent_jobs }) => ({
          processor,
          profile,
          max_concurrent_jobs,
        })),
    }));
  } catch (err) {
    if (isSchemaNotReady(err)) return [];
    throw err;
  }
}

/**
 * Delete a worker via the REST API.
 * @param {string} workerId
 * @returns {Promise<void>}
 */
export async function deleteWorker(workerId) {
  const resp = await fetch(`${REST_BASE}/workers/${workerId}`, {
    method: "DELETE",
    headers: hpcHeaders(),
  });
  if (!resp.ok) {
    const err = await resp.json().catch(() => ({}));
    throw new Error(err.detail || `HTTP ${resp.status}`);
  }
}

// --- Artifact API ---

/**
 * Fetch artifacts via GraphQL.
 * @param {Object} opts - { status, limit, offset }
 */
export async function fetchArtifacts({ status, limit = 50, offset = 0 } = {}) {
  const filters = [];
  if (status) {
    filters.push(`status: { name: { equals: ${gqlString(status)} } }`);
  }
  const filterClause = filters.length
    ? `filter: { ${filters.join(", ")} }`
    : "";
  const aggLine = filterClause
    ? `HpcArtifacts_agg(${filterClause}) { count }`
    : `HpcArtifacts_agg { count }`;

  const query = `{
    HpcArtifacts(
      ${filterClause ? filterClause + "," : ""}
      limit: ${limit},
      offset: ${offset},
      orderby: { created_at: DESC }
    ) {
      id name type
      residence { name }
      status { name }
      sha256 size_bytes content_url metadata
      created_at committed_at
    }
    ${aggLine}
  }`;

  try {
    const data = await request(GRAPHQL_URL, query);
    return {
      items: (data.HpcArtifacts || []).map(normalizeArtifact),
      totalCount: data.HpcArtifacts_agg?.count ?? 0,
    };
  } catch (err) {
    if (isSchemaNotReady(err)) {
      return { items: [], totalCount: 0, schemaNotReady: true };
    }
    throw err;
  }
}

/**
 * Fetch a single artifact with its files via GraphQL.
 * @param {string} artifactId
 */
export async function fetchArtifactDetail(artifactId) {
  const query = `{
    HpcArtifacts(filter: { id: { equals: ${gqlString(artifactId)} } }) {
      id name type
      residence { name }
      status { name }
      sha256 size_bytes content_url metadata
      created_at committed_at
    }
    HpcArtifactFiles(
      filter: { artifact_id: { id: { equals: ${gqlString(artifactId)} } } }
    ) {
      id path sha256 size_bytes content_type
    }
  }`;

  try {
    const data = await request(GRAPHQL_URL, query);
    const artifact = data.HpcArtifacts?.[0];
    return {
      artifact: artifact ? normalizeArtifact(artifact) : null,
      files: data.HpcArtifactFiles || [],
    };
  } catch (err) {
    if (isSchemaNotReady(err)) {
      return { artifact: null, files: [], schemaNotReady: true };
    }
    throw err;
  }
}

/**
 * Create a new artifact via the REST API.
 * @param {Object} opts - { name, type, residence }
 */
export async function createArtifact({ name, type = "blob", residence = "managed", content_url } = {}) {
  const body = { type, residence };
  if (name) body.name = name;
  if (content_url) body.content_url = content_url;
  const resp = await fetch(`${REST_BASE}/artifacts`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...hpcHeaders(),
    },
    body: JSON.stringify(body),
  });
  if (!resp.ok) {
    const err = await resp.json().catch(() => ({}));
    throw new Error(err.detail || `HTTP ${resp.status}`);
  }
  return resp.json();
}

/**
 * Upload a file to an artifact via PUT.
 * @param {string} artifactId
 * @param {File} file - browser File object
 * @param {string} [path] - path in artifact (defaults to file.name)
 */
export async function uploadArtifactFile(artifactId, file, path) {
  const filePath = path || file.name;
  const resp = await fetch(`${REST_BASE}/artifacts/${artifactId}/files/${filePath}`, {
    method: "PUT",
    headers: {
      "Content-Type": file.type || "application/octet-stream",
      ...hpcHeaders(),
    },
    body: file,
  });
  if (!resp.ok) {
    const err = await resp.json().catch(() => ({}));
    throw new Error(err.detail || `HTTP ${resp.status}`);
  }
  return resp.json();
}

/**
 * Commit an artifact with final hash and size.
 * @param {string} artifactId
 * @param {Object} opts - { sha256, size_bytes }
 */
export async function commitArtifact(artifactId, { sha256, size_bytes } = {}) {
  const body = {};
  if (sha256) body.sha256 = sha256;
  if (size_bytes != null) body.size_bytes = size_bytes;
  const resp = await fetch(`${REST_BASE}/artifacts/${artifactId}/commit`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...hpcHeaders(),
    },
    body: JSON.stringify(body),
  });
  if (!resp.ok) {
    const err = await resp.json().catch(() => ({}));
    throw new Error(err.detail || `HTTP ${resp.status}`);
  }
  return resp.json();
}

/**
 * Build a download URL for an artifact file.
 * @param {string} artifactId
 * @param {string} filePath
 * @returns {string}
 */
export async function deleteArtifact(artifactId) {
  const resp = await fetch(`${REST_BASE}/artifacts/${artifactId}`, {
    method: "DELETE",
    headers: hpcHeaders(),
  });
  if (!resp.ok) {
    const err = await resp.json().catch(() => ({}));
    throw new Error(err.detail || `HTTP ${resp.status}`);
  }
}

export function artifactFileDownloadUrl(artifactId, filePath) {
  return `${REST_BASE}/artifacts/${artifactId}/files/${filePath}`;
}

/**
 * Download an artifact file using fetch (includes required protocol headers).
 * Triggers a browser file-save dialog.
 */
export async function downloadArtifactFile(artifactId, filePath) {
  const url = artifactFileDownloadUrl(artifactId, filePath);
  const resp = await fetch(url, { headers: hpcHeaders() });
  if (!resp.ok) {
    const text = await resp.text();
    throw new Error(`Download failed: ${resp.status} ${text}`);
  }
  const blob = await resp.blob();
  const a = document.createElement("a");
  a.href = URL.createObjectURL(blob);
  a.download = filePath.split("/").pop() || filePath;
  a.click();
  URL.revokeObjectURL(a.href);
}

/** Flatten REF/ONTOLOGY objects to plain strings. */
function normalizeJob(job) {
  const output = job.output_artifact_id;
  const log = job.log_artifact_id;
  return {
    ...job,
    status: job.status?.name ?? job.status,
    worker_id: job.worker_id?.worker_id ?? job.worker_id,
    output_artifact_id: output
      ? {
          id: output.id,
          name: output.name,
          type: output.type,
          status: output.status?.name ?? output.status,
        }
      : null,
    log_artifact_id: log
      ? {
          id: log.id,
          name: log.name,
          type: log.type,
          status: log.status?.name ?? log.status,
        }
      : null,
  };
}

/** Flatten REF/ONTOLOGY objects for artifacts. */
function normalizeArtifact(artifact) {
  return {
    ...artifact,
    type: artifact.type,
    residence: artifact.residence?.name ?? artifact.residence,
    status: artifact.status?.name ?? artifact.status,
  };
}
