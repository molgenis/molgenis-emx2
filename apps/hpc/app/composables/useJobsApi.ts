import { resolveProgressSnapshot } from "../utils/jobs";
import {
  gqlString,
  isSchemaNotReady,
  gqlQuery,
  hpcHeaders,
  REST_BASE,
} from "./useHpcApi";

export interface FetchJobsOpts {
  status?: string;
  processor?: string;
  limit?: number;
  offset?: number;
}

export interface NormalizedJob {
  id: string;
  processor: string;
  profile: string;
  status: string;
  worker_id: string | null;
  phase: string | null;
  message: string | null;
  progress: number | null;
  output_artifact_id: any;
  log_artifact_id: any;
  slurm_job_id: string | null;
  submit_user: string | null;
  created_at: string;
  updated_at: string;
  parameters: any;
  inputs: any[];
  [key: string]: any;
}

const inputArtifactCache = new Map<
  string,
  {
    id: string;
    name?: string | null;
    type?: string | null;
    status?: string | null;
    residence?: string | null;
  } | null
>();

function parseInputsValue(rawInputs: unknown): any[] {
  if (rawInputs == null) return [];
  if (Array.isArray(rawInputs)) return rawInputs;
  if (typeof rawInputs === "string") {
    const trimmed = rawInputs.trim();
    if (!trimmed) return [];
    try {
      const parsed = JSON.parse(trimmed);
      return Array.isArray(parsed) ? parsed : [parsed];
    } catch {
      return [];
    }
  }
  return [rawInputs];
}

function normalizeInputRef(item: any): any {
  if (typeof item === "string") {
    const trimmed = item.trim();
    return trimmed ? { id: trimmed } : item;
  }

  if (item && typeof item === "object") {
    if (typeof item.id === "string" && item.id) return item;
    if (typeof item.artifact_id === "string" && item.artifact_id) {
      return { ...item, id: item.artifact_id };
    }
  }

  return item;
}

function collectInputArtifactIds(jobs: NormalizedJob[]): string[] {
  const ids = new Set<string>();
  for (const job of jobs) {
    const inputs = Array.isArray(job.inputs) ? job.inputs : [];
    for (const input of inputs) {
      const id =
        typeof input === "string"
          ? input
          : input?.id || input?.artifact_id || null;
      if (typeof id === "string" && id.trim()) ids.add(id);
    }
  }
  return [...ids];
}

async function fetchArtifactSummary(id: string): Promise<{
  id: string;
  name?: string | null;
  type?: string | null;
  status?: string | null;
  residence?: string | null;
} | null> {
  try {
    const artifact = await $fetch<any>(`${REST_BASE}/artifacts/${id}`, {
      method: "GET",
      headers: hpcHeaders(),
    });
    return {
      id,
      name: artifact?.name ?? null,
      type: artifact?.type ?? null,
      status: artifact?.status ?? null,
      residence: artifact?.residence ?? null,
    };
  } catch {
    return null;
  }
}

async function enrichInputArtifactCache(ids: string[]): Promise<void> {
  const missing = ids.filter((id) => !inputArtifactCache.has(id));
  if (!missing.length) return;

  const resolved = await Promise.all(
    missing.map(async (id) => [id, await fetchArtifactSummary(id)] as const)
  );
  for (const [id, summary] of resolved) {
    inputArtifactCache.set(id, summary);
  }
}

function enrichInputsWithNames(inputs: any[]): any[] {
  return inputs.map((input) => {
    const id =
      typeof input === "string" ? input : input?.id || input?.artifact_id || null;
    if (typeof id !== "string" || !id) return input;

    const cached = inputArtifactCache.get(id);
    if (!cached) {
      return typeof input === "string" ? { id } : { ...input, id };
    }

    const base = typeof input === "string" ? { id } : { ...input, id };
    return {
      ...cached,
      ...base,
      name: base.name ?? cached.name ?? null,
      type: base.type ?? cached.type ?? null,
      status: base.status ?? cached.status ?? null,
      residence: base.residence ?? cached.residence ?? null,
    };
  });
}

async function enrichJobsInputs(jobs: NormalizedJob[]): Promise<NormalizedJob[]> {
  const ids = collectInputArtifactIds(jobs);
  await enrichInputArtifactCache(ids);
  return jobs.map((job) => ({
    ...job,
    inputs: enrichInputsWithNames(Array.isArray(job.inputs) ? job.inputs : []),
  }));
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
      phase message progress
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
    const normalized = (data.HpcJobs || []).map(normalizeJob);
    const enriched = await enrichJobsInputs(normalized);
    return {
      items: enriched,
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
      phase message progress
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
      id from_status to_status timestamp worker_id detail phase message progress
    }
  }`;

  try {
    const data = await gqlQuery(query);
    const job = data.HpcJobs?.[0];
    const normalized = job ? normalizeJob(job) : null;
    const enrichedJobs = normalized ? await enrichJobsInputs([normalized]) : [];
    return {
      job: enrichedJobs[0] ?? null,
      transitions: (data.HpcJobTransitions || []).map(normalizeTransition),
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

/** Flatten REF/ONTOLOGY objects to plain strings. */
export function normalizeJob(job: any): NormalizedJob {
  const output = job.output_artifact_id;
  const log = job.log_artifact_id;
  const progressSnapshot = resolveProgressSnapshot(job);
  const lifecycleTimestamps = [
    job.created_at,
    job.claimed_at,
    job.submitted_at,
    job.started_at,
    job.completed_at,
  ]
    .filter(Boolean)
    .sort(
      (a: string, b: string) => new Date(a).getTime() - new Date(b).getTime()
    );
  const updatedAt =
    lifecycleTimestamps[lifecycleTimestamps.length - 1] ?? job.created_at;
  const normalizedInputs = parseInputsValue(job.inputs).map(normalizeInputRef);

  return {
    ...job,
    status: job.status?.name ?? job.status,
    worker_id: job.worker_id?.worker_id ?? job.worker_id,
    phase: progressSnapshot.phase,
    message: progressSnapshot.message,
    progress: progressSnapshot.progress,
    updated_at: updatedAt,
    inputs: normalizedInputs,
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

export function normalizeTransition(transition: any): any {
  const progressSnapshot = resolveProgressSnapshot(transition);
  return {
    ...transition,
    from_status: transition.from_status?.name ?? transition.from_status,
    to_status: transition.to_status?.name ?? transition.to_status,
    worker_id: transition.worker_id?.worker_id ?? transition.worker_id,
    phase: progressSnapshot.phase,
    message: progressSnapshot.message,
    progress: progressSnapshot.progress,
  };
}
