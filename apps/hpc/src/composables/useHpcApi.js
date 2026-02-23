import { request } from "graphql-request";

const GRAPHQL_URL = "/_SYSTEM_/graphql";
const REST_BASE = "/api/hpc";

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
    filters.push(`status: { name: { equals: "${status}" } }`);
  }
  if (processor) {
    filters.push(`processor: { equals: "${processor}" }`);
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
      output_artifact_id { id type { name } status { name } }
      slurm_job_id submit_user
      created_at claimed_at submitted_at started_at completed_at
      parameters inputs
    }
    ${aggLine}
  }`;

  const data = await request(GRAPHQL_URL, query);
  return {
    items: (data.HpcJobs || []).map(normalizeJob),
    totalCount: data.HpcJobs_agg?.count ?? 0,
  };
}

/**
 * Fetch a single job with its transitions.
 * @param {string} jobId
 */
export async function fetchJobDetail(jobId) {
  const query = `{
    HpcJobs(filter: { id: { equals: "${jobId}" } }) {
      id processor profile
      status { name }
      worker_id { worker_id }
      output_artifact_id { id type { name } status { name } }
      slurm_job_id submit_user
      created_at claimed_at submitted_at started_at completed_at
      parameters inputs
    }
    HpcJobTransitions(
      filter: { job_id: { id: { equals: "${jobId}" } } }
      orderby: { timestamp: ASC }
    ) {
      id from_status to_status timestamp worker_id detail
    }
  }`;

  const data = await request(GRAPHQL_URL, query);
  const job = data.HpcJobs?.[0];
  return {
    job: job ? normalizeJob(job) : null,
    transitions: data.HpcJobTransitions || [],
  };
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
}

// --- Artifact API ---

/**
 * Fetch artifacts via GraphQL.
 * @param {Object} opts - { status, limit, offset }
 */
export async function fetchArtifacts({ status, limit = 50, offset = 0 } = {}) {
  const filters = [];
  if (status) {
    filters.push(`status: { name: { equals: "${status}" } }`);
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
      id
      type { name }
      format
      residence { name }
      status { name }
      sha256 size_bytes content_url metadata
      created_at committed_at
    }
    ${aggLine}
  }`;

  const data = await request(GRAPHQL_URL, query);
  return {
    items: (data.HpcArtifacts || []).map(normalizeArtifact),
    totalCount: data.HpcArtifacts_agg?.count ?? 0,
  };
}

/**
 * Fetch a single artifact with its files via GraphQL.
 * @param {string} artifactId
 */
export async function fetchArtifactDetail(artifactId) {
  const query = `{
    HpcArtifacts(filter: { id: { equals: "${artifactId}" } }) {
      id
      type { name }
      format
      residence { name }
      status { name }
      sha256 size_bytes content_url metadata
      created_at committed_at
    }
    HpcArtifactFiles(
      filter: { artifact_id: { id: { equals: "${artifactId}" } } }
    ) {
      id path role sha256 size_bytes content_type
    }
  }`;

  const data = await request(GRAPHQL_URL, query);
  const artifact = data.HpcArtifacts?.[0];
  return {
    artifact: artifact ? normalizeArtifact(artifact) : null,
    files: data.HpcArtifactFiles || [],
  };
}

/**
 * Create a new artifact via the REST API.
 * @param {Object} opts - { type, format, residence }
 */
export async function createArtifact({ type = "blob", format, residence = "managed" } = {}) {
  const body = { type, residence };
  if (format) body.format = format;
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
export function artifactFileDownloadUrl(artifactId, filePath) {
  return `${REST_BASE}/artifacts/${artifactId}/files/${filePath}`;
}

/** Flatten REF/ONTOLOGY objects to plain strings. */
function normalizeJob(job) {
  const output = job.output_artifact_id;
  return {
    ...job,
    status: job.status?.name ?? job.status,
    worker_id: job.worker_id?.worker_id ?? job.worker_id,
    output_artifact_id: output
      ? {
          id: output.id,
          type: output.type?.name ?? output.type,
          status: output.status?.name ?? output.status,
        }
      : null,
  };
}

/** Flatten REF/ONTOLOGY objects for artifacts. */
function normalizeArtifact(artifact) {
  return {
    ...artifact,
    type: artifact.type?.name ?? artifact.type,
    residence: artifact.residence?.name ?? artifact.residence,
    status: artifact.status?.name ?? artifact.status,
  };
}
