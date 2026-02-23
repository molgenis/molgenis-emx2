import { request } from "graphql-request";

const GRAPHQL_URL = "/_SYSTEM_/graphql";
const REST_BASE = "/api/hpc";

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
 * @param {Object} payload - { processor, profile, parameters, submit_user }
 */
export async function submitJob(payload) {
  const resp = await fetch(`${REST_BASE}/jobs`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-EMX2-API-Version": "2025-01",
      "X-Request-Id": crypto.randomUUID(),
      "X-Timestamp": new Date().toISOString(),
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
    headers: {
      "X-EMX2-API-Version": "2025-01",
      "X-Request-Id": crypto.randomUUID(),
      "X-Timestamp": new Date().toISOString(),
    },
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

/** Flatten REF/ONTOLOGY objects to plain strings. */
function normalizeJob(job) {
  return {
    ...job,
    status: job.status?.name ?? job.status,
    worker_id: job.worker_id?.worker_id ?? job.worker_id,
  };
}
