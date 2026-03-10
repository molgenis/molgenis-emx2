import { API_VERSION } from "../utils/protocol";

const GRAPHQL_URL = "/_SYSTEM_/graphql";
const REST_BASE = "/api/hpc";

/** Safely encode a JS string as a GraphQL string literal. */
function gqlString(value: string): string {
  return JSON.stringify(String(value));
}

/**
 * Returns true if the error indicates HPC tables don't exist yet
 * (e.g. FieldUndefined for HpcJobs, HpcWorkers, etc.).
 */
function isSchemaNotReady(err: any): boolean {
  const msg = err?.response?.errors?.[0]?.message ?? err?.message ?? "";
  return msg.includes("FieldUndefined") || msg.includes("is undefined");
}

/** Execute a GraphQL query against the _SYSTEM_ schema. */
async function gqlQuery(query: string): Promise<any> {
  const resp = await $fetch<any>(GRAPHQL_URL, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: { query },
  });
  if (resp.errors?.length) {
    throw new Error(resp.errors[0].message);
  }
  return resp.data ?? resp;
}

/**
 * Check whether HPC is enabled via the health endpoint.
 */
export async function fetchHpcHealth(): Promise<{
  ok: boolean;
  hpc_enabled: boolean;
  database: string;
} | null> {
  try {
    return await $fetch(`${REST_BASE}/health`);
  } catch {
    return null;
  }
}

/** Common REST headers for HPC API calls. */
function hpcHeaders(): Record<string, string> {
  return {
    "X-EMX2-API-Version": API_VERSION,
    "X-Request-Id": crypto.randomUUID(),
    "X-Timestamp": String(Math.floor(Date.now() / 1000)),
  };
}

function toHex(bytes: Uint8Array): string {
  return Array.from(bytes, (byte) => byte.toString(16).padStart(2, "0")).join(
    ""
  );
}

async function sha256Hex(blob: Blob): Promise<string> {
  const data = await blob.arrayBuffer();
  const digest = await crypto.subtle.digest("SHA-256", data);
  return toHex(new Uint8Array(digest));
}

interface FetchJobsOpts {
  status?: string;
  processor?: string;
  limit?: number;
  offset?: number;
}

interface NormalizedJob {
  id: string;
  processor: string;
  profile: string;
  status: string;
  worker_id: string | null;
  output_artifact_id: any;
  log_artifact_id: any;
  slurm_job_id: string | null;
  submit_user: string | null;
  created_at: string;
  parameters: any;
  inputs: any[];
  [key: string]: any;
}

/**
 * Fetch jobs from the SYSTEM schema via GraphQL.
 */
export async function fetchJobs({
  status,
  processor,
  limit = 50,
  offset = 0,
}: FetchJobsOpts = {}): Promise<{
  items: NormalizedJob[];
  totalCount: number;
  schemaNotReady?: boolean;
}> {
  const filters: string[] = [];
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
      output_artifact_id { id name type residence { name } status { name } }
      log_artifact_id { id name type residence { name } status { name } }
      slurm_job_id submit_user
      created_at claimed_at submitted_at started_at completed_at
      parameters inputs
    }
    ${aggLine}
  }`;

  try {
    const data = await gqlQuery(query);
    return {
      items: (data.HpcJobs || []).map(normalizeJob),
      totalCount: data.HpcJobs_agg?.count ?? 0,
    };
  } catch (err: any) {
    if (isSchemaNotReady(err)) {
      return { items: [], totalCount: 0, schemaNotReady: true };
    }
    throw err;
  }
}

/**
 * Fetch a single job with its transitions.
 */
export async function fetchJobDetail(jobId: string): Promise<{
  job: NormalizedJob | null;
  transitions: any[];
  schemaNotReady?: boolean;
}> {
  const query = `{
    HpcJobs(filter: { id: { equals: ${gqlString(jobId)} } }) {
      id processor profile
      status { name }
      worker_id { worker_id }
      output_artifact_id { id name type residence { name } status { name } }
      log_artifact_id { id name type residence { name } status { name } }
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
    const data = await gqlQuery(query);
    const job = data.HpcJobs?.[0];
    return {
      job: job ? normalizeJob(job) : null,
      transitions: data.HpcJobTransitions || [],
    };
  } catch (err: any) {
    if (isSchemaNotReady(err)) {
      return { job: null, transitions: [], schemaNotReady: true };
    }
    throw err;
  }
}

/**
 * Submit a new job via the REST API.
 */
export async function submitJob(payload: {
  processor: string;
  profile?: string;
  parameters?: any;
  inputs?: string[];
}): Promise<any> {
  return await $fetch(`${REST_BASE}/jobs`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...hpcHeaders(),
    },
    body: payload,
  });
}

/**
 * Cancel an active job via the REST API.
 */
export async function cancelJob(jobId: string): Promise<void> {
  await $fetch(`${REST_BASE}/jobs/${jobId}/cancel`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...hpcHeaders(),
    },
    body: {},
  });
}

/**
 * Delete a terminal job via the REST API.
 */
export async function deleteJob(jobId: string): Promise<void> {
  await $fetch(`${REST_BASE}/jobs/${jobId}`, {
    method: "DELETE",
    headers: hpcHeaders(),
  });
}

/**
 * Fetch registered workers with their capabilities via GraphQL.
 */
export async function fetchWorkers(): Promise<any[]> {
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
    const data = await gqlQuery(query);
    const workers = data.HpcWorkers || [];
    const caps = data.HpcWorkerCapabilities || [];

    return workers.map((w: any) => ({
      ...w,
      capabilities: caps
        .filter(
          (c: any) => (c.worker_id?.worker_id ?? c.worker_id) === w.worker_id
        )
        .map(
          ({
            processor,
            profile,
            max_concurrent_jobs,
          }: {
            processor: string;
            profile: string;
            max_concurrent_jobs: number;
          }) => ({
            processor,
            profile,
            max_concurrent_jobs,
          })
        ),
    }));
  } catch (err: any) {
    if (isSchemaNotReady(err)) return [];
    throw err;
  }
}

/**
 * Fetch distinct processor/profile pairs from worker capabilities.
 */
export async function fetchCapabilities(): Promise<
  { processor: string; profile: string }[]
> {
  const query = `{
    HpcWorkerCapabilities {
      processor profile
    }
  }`;

  try {
    const data = await gqlQuery(query);
    const caps = data.HpcWorkerCapabilities || [];
    const seen = new Set<string>();
    return caps.filter((c: any) => {
      const key = `${c.processor}\0${c.profile}`;
      if (seen.has(key)) return false;
      seen.add(key);
      return true;
    });
  } catch (err: any) {
    if (isSchemaNotReady(err)) return [];
    throw err;
  }
}

/**
 * Delete a worker via the REST API.
 */
export async function deleteWorker(workerId: string): Promise<void> {
  await $fetch(`${REST_BASE}/workers/${workerId}`, {
    method: "DELETE",
    headers: hpcHeaders(),
  });
}

// --- Artifact API ---

interface FetchArtifactsOpts {
  status?: string;
  limit?: number;
  offset?: number;
}

/**
 * Fetch artifacts via GraphQL.
 */
export async function fetchArtifacts({
  status,
  limit = 50,
  offset = 0,
}: FetchArtifactsOpts = {}): Promise<{
  items: any[];
  totalCount: number;
  schemaNotReady?: boolean;
}> {
  const filters: string[] = [];
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
    const data = await gqlQuery(query);
    return {
      items: (data.HpcArtifacts || []).map(normalizeArtifact),
      totalCount: data.HpcArtifacts_agg?.count ?? 0,
    };
  } catch (err: any) {
    if (isSchemaNotReady(err)) {
      return { items: [], totalCount: 0, schemaNotReady: true };
    }
    throw err;
  }
}

/**
 * Fetch a single artifact with its files via GraphQL.
 */
export async function fetchArtifactDetail(artifactId: string): Promise<{
  artifact: any;
  files: any[];
  schemaNotReady?: boolean;
}> {
  const idFilter = gqlString(artifactId);
  const query = `{
    HpcArtifacts(filter: { id: { equals: ${idFilter} } }) {
      id name type
      residence { name }
      status { name }
      sha256 size_bytes content_url metadata
      created_at committed_at
    }
    HpcArtifactFiles(
      filter: { artifact_id: { id: { equals: ${idFilter} } } }
    ) {
      id path sha256 size_bytes content_type
    }
  }`;

  try {
    const data = await gqlQuery(query);
    const artifact = data.HpcArtifacts?.[0];
    return {
      artifact: artifact ? normalizeArtifact(artifact) : null,
      files: data.HpcArtifactFiles || [],
    };
  } catch (err: any) {
    if (isSchemaNotReady(err)) {
      return { artifact: null, files: [], schemaNotReady: true };
    }
    throw err;
  }
}

/**
 * Create a new artifact via the REST API.
 */
export async function createArtifact({
  name,
  type = "blob",
  residence = "managed",
  content_url,
}: {
  name?: string;
  type?: string;
  residence?: string;
  content_url?: string;
} = {}): Promise<any> {
  const body: any = { type, residence };
  if (name) body.name = name;
  if (content_url) body.content_url = content_url;
  return await $fetch(`${REST_BASE}/artifacts`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...hpcHeaders(),
    },
    body,
  });
}

/**
 * Upload a file to an artifact via PUT.
 */
export async function uploadArtifactFile(
  artifactId: string,
  file: File,
  path?: string
): Promise<any> {
  const filePath = path || file.name;
  const contentSha256 = await sha256Hex(file);
  return await $fetch(
    `${REST_BASE}/artifacts/${artifactId}/files/${filePath}`,
    {
      method: "PUT",
      headers: {
        "Content-Type": file.type || "application/octet-stream",
        "Content-SHA256": contentSha256,
        ...hpcHeaders(),
      },
      body: file,
    }
  );
}

/**
 * Commit an artifact with final hash and size.
 */
export async function commitArtifact(
  artifactId: string,
  { sha256, size_bytes }: { sha256?: string; size_bytes?: number } = {}
): Promise<any> {
  const body: any = {};
  if (sha256) body.sha256 = sha256;
  if (size_bytes != null) body.size_bytes = size_bytes;
  return await $fetch(`${REST_BASE}/artifacts/${artifactId}/commit`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...hpcHeaders(),
    },
    body,
  });
}

/**
 * Delete an artifact via the REST API.
 */
export async function deleteArtifact(artifactId: string): Promise<void> {
  await $fetch(`${REST_BASE}/artifacts/${artifactId}`, {
    method: "DELETE",
    headers: hpcHeaders(),
  });
}

/**
 * Build a download URL for an artifact file.
 */
export function artifactFileDownloadUrl(
  artifactId: string,
  filePath: string
): string {
  return `${REST_BASE}/artifacts/${artifactId}/files/${filePath}`;
}

/**
 * Download an artifact file using fetch (includes required protocol headers).
 * Triggers a browser file-save dialog.
 */
export async function downloadArtifactFile(
  artifactId: string,
  filePath: string
): Promise<void> {
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
function normalizeJob(job: any): NormalizedJob {
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
          residence: output.residence?.name ?? output.residence,
          status: output.status?.name ?? output.status,
        }
      : null,
    log_artifact_id: log
      ? {
          id: log.id,
          name: log.name,
          type: log.type,
          residence: log.residence?.name ?? log.residence,
          status: log.status?.name ?? log.status,
        }
      : null,
  };
}

/** Flatten REF/ONTOLOGY objects for artifacts and parse metadata JSON. */
function normalizeArtifact(artifact: any): any {
  let metadata = artifact.metadata;
  if (typeof metadata === "string") {
    try {
      metadata = JSON.parse(metadata);
    } catch {
      // leave as string
    }
  }
  return {
    ...artifact,
    type: artifact.type,
    residence: artifact.residence?.name ?? artifact.residence,
    status: artifact.status?.name ?? artifact.status,
    metadata,
  };
}
