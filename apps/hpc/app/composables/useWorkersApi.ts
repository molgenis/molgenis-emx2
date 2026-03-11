import {
  gqlQuery,
  isSchemaNotReady,
  hpcHeaders,
  REST_BASE,
  ACTIVE_JOB_STATUSES,
} from "./useHpcApi";
import { normalizeJob } from "./useJobsApi";

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
    HpcJobs(limit: 500, orderby: { created_at: DESC }) {
      id processor profile
      status { name }
      worker_id { worker_id }
      phase message progress
      created_at claimed_at submitted_at started_at completed_at
    }
  }`;

  try {
    const data = await gqlQuery(query);
    const workers = data.HpcWorkers || [];
    const caps = data.HpcWorkerCapabilities || [];
    const normalizedJobs = (data.HpcJobs || []).map(normalizeJob);
    const activeJobsByWorker = new Map<string, any[]>();

    for (const job of normalizedJobs) {
      if (!ACTIVE_JOB_STATUSES.has(job.status) || !job.worker_id) continue;
      const existing = activeJobsByWorker.get(job.worker_id) || [];
      existing.push(job);
      activeJobsByWorker.set(job.worker_id, existing);
    }

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
      active_jobs: (activeJobsByWorker.get(w.worker_id) || []).sort(
        (a: any, b: any) =>
          new Date(b.updated_at).getTime() - new Date(a.updated_at).getTime()
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

export type WorkerCredential = {
  id: string;
  worker_id: string;
  status: "ACTIVE" | "REVOKED" | "EXPIRED" | string;
  label?: string | null;
  created_at?: string | null;
  created_by?: string | null;
  last_used_at?: string | null;
  revoked_at?: string | null;
  expires_at?: string | null;
};

export async function fetchWorkerCredentials(
  workerId: string
): Promise<WorkerCredential[]> {
  const result = await $fetch<any>(`${REST_BASE}/workers/${workerId}/credentials`, {
    method: "GET",
    headers: hpcHeaders(),
  });
  return (result?.items || []) as WorkerCredential[];
}

export async function issueWorkerCredential(
  workerId: string,
  payload: { label?: string; expires_at?: string } = {}
): Promise<{ id: string; secret: string }> {
  return await $fetch<any>(`${REST_BASE}/workers/${workerId}/credentials/issue`, {
    method: "POST",
    headers: hpcHeaders(),
    body: payload,
  });
}

export async function rotateWorkerCredential(
  workerId: string,
  payload: { label?: string; expires_at?: string } = {}
): Promise<{ id: string; secret: string }> {
  return await $fetch<any>(`${REST_BASE}/workers/${workerId}/credentials/rotate`, {
    method: "POST",
    headers: hpcHeaders(),
    body: payload,
  });
}

export async function revokeWorkerCredential(
  workerId: string,
  credentialId: string
): Promise<void> {
  await $fetch<any>(
    `${REST_BASE}/workers/${workerId}/credentials/${credentialId}/revoke`,
    {
      method: "POST",
      headers: hpcHeaders(),
    }
  );
}
