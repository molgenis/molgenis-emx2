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
  output_artifact_id: NormalizedArtifactRef | null;
  log_artifact_id: NormalizedArtifactRef | null;
  slurm_job_id: string | null;
  submit_user: string | null;
  created_at: string;
  updated_at: string;
  parameters: unknown;
  inputs: NormalizedJobInput[];
  [key: string]: unknown;
}

type ArtifactRefRelation = { name?: string | null };

type NormalizedArtifactRef = {
  id: string;
  name?: string | null;
  type?: string | null;
  status?: string | null;
  residence?: string | null;
};

type NormalizedJobInput = {
  id?: string;
  artifact_id?: string;
  name?: string | null;
  type?: string | null;
  status?: string | null;
  residence?: string | null;
  [key: string]: unknown;
};

type JobGraphqlRow = {
  id: string;
  processor: string;
  profile: string;
  status: string | { name?: string | null };
  worker_id: string | { worker_id?: string | null } | null;
  phase?: string | null;
  message?: string | null;
  progress?: number | null;
  output_artifact_id?: (NormalizedArtifactRef & {
    residence?: string | ArtifactRefRelation | null;
    status?: string | ArtifactRefRelation | null;
  }) | null;
  log_artifact_id?: (NormalizedArtifactRef & {
    residence?: string | ArtifactRefRelation | null;
    status?: string | ArtifactRefRelation | null;
  }) | null;
  slurm_job_id?: string | null;
  submit_user?: string | null;
  created_at: string;
  claimed_at?: string | null;
  submitted_at?: string | null;
  started_at?: string | null;
  completed_at?: string | null;
  parameters?: unknown;
  inputs?: unknown;
};

type TransitionGraphqlRow = {
  id: string;
  from_status?: string | { name?: string | null } | null;
  to_status?: string | { name?: string | null } | null;
  timestamp: string;
  worker_id?: string | { worker_id?: string | null } | null;
  detail?: string | null;
  phase?: string | null;
  message?: string | null;
  progress?: number | null;
};

export type NormalizedTransition = TransitionGraphqlRow & {
  from_status?: string | null;
  to_status?: string | null;
  worker_id?: string | null;
};

type JobsQueryResponse = {
  HpcJobs?: JobGraphqlRow[];
  HpcJobs_agg?: { count?: number };
};

type JobDetailResponse = {
  HpcJobs?: JobGraphqlRow[];
  HpcJobTransitions?: TransitionGraphqlRow[];
};

type SubmitJobPayload = {
  processor: string;
  profile?: string;
  parameters?: unknown;
  inputs?: string[];
  timeout_seconds?: number;
};

const inputArtifactCache = new Map<string, NormalizedArtifactRef | null>();

function parseInputsValue(rawInputs: unknown): NormalizedJobInput[] {
  if (rawInputs == null) return [];
  if (Array.isArray(rawInputs)) return rawInputs.map((item) => normalizeInputRef(item));
  if (typeof rawInputs === "string") {
    const trimmed = rawInputs.trim();
    if (!trimmed) return [];
    try {
      const parsed = JSON.parse(trimmed);
      return Array.isArray(parsed)
        ? parsed.map((item) => normalizeInputRef(item))
        : [normalizeInputRef(parsed)];
    } catch {
      return [];
    }
  }
  return [normalizeInputRef(rawInputs)];
}

function normalizeInputRef(item: unknown): NormalizedJobInput {
  if (typeof item === "string") {
    const trimmed = item.trim();
    return trimmed ? { id: trimmed } : {};
  }

  if (item && typeof item === "object") {
    const input = item as NormalizedJobInput;
    if (typeof input.id === "string" && input.id) return input;
    if (typeof input.artifact_id === "string" && input.artifact_id) {
      return { ...input, id: input.artifact_id };
    }
    return input;
  }

  return {};
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
    const artifact = await $fetch<NormalizedArtifactRef>(
      `${REST_BASE}/artifacts/${id}`,
      {
      method: "GET",
      headers: hpcHeaders(),
      }
    );
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

function enrichInputsWithNames(inputs: NormalizedJobInput[]): NormalizedJobInput[] {
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

async function enrichJobsInputs(
  jobs: NormalizedJob[]
): Promise<NormalizedJob[]> {
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
      ${filterClause ? `${filterClause},` : ""}
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
    const data = await gqlQuery<JobsQueryResponse>(query);
    const normalized = (data.HpcJobs || []).map(normalizeJob);
    const enriched = await enrichJobsInputs(normalized);
    return {
      items: enriched,
      totalCount: data.HpcJobs_agg?.count ?? 0,
    };
  } catch (err: unknown) {
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
  transitions: NormalizedTransition[];
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
    const data = await gqlQuery<JobDetailResponse>(query);
    const job = data.HpcJobs?.[0];
    const normalized = job ? normalizeJob(job) : null;
    const enrichedJobs = normalized ? await enrichJobsInputs([normalized]) : [];
    return {
      job: enrichedJobs[0] ?? null,
      transitions: (data.HpcJobTransitions || []).map(normalizeTransition),
    };
  } catch (err: unknown) {
    if (isSchemaNotReady(err)) {
      return { job: null, transitions: [], schemaNotReady: true };
    }
    throw err;
  }
}

/**
 * Submit a new job via the REST API.
 */
export async function submitJob(
  payload: SubmitJobPayload
): Promise<NormalizedJob> {
  return await $fetch<NormalizedJob>(`${REST_BASE}/jobs`, {
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
export function normalizeJob(job: JobGraphqlRow): NormalizedJob {
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

export function normalizeTransition(
  transition: TransitionGraphqlRow
): NormalizedTransition {
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
