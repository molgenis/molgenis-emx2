import { API_VERSION } from "../utils/protocol";

export const GRAPHQL_URL = "/_SYSTEM_/graphql";
export const REST_BASE = "/api/hpc";
export const ACTIVE_JOB_STATUSES = new Set(["CLAIMED", "SUBMITTED", "STARTED"]);

/** Safely encode a JS string as a GraphQL string literal. */
export function gqlString(value: string): string {
  return JSON.stringify(String(value));
}

/**
 * Returns true if the error indicates HPC tables don't exist yet
 * (e.g. FieldUndefined for HpcJobs, HpcWorkers, etc.).
 */
export function isSchemaNotReady(err: any): boolean {
  const msg = err?.response?.errors?.[0]?.message ?? err?.message ?? "";
  return msg.includes("FieldUndefined") || msg.includes("is undefined");
}

/** Execute a GraphQL query against the _SYSTEM_ schema. */
export async function gqlQuery(query: string): Promise<any> {
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
  status?: string;
  ok?: boolean;
  hpc_enabled: boolean;
  hpc_initialized?: boolean;
  worker_auth_mode?: string;
  database: string;
} | null> {
  try {
    return await $fetch(`${REST_BASE}/health`);
  } catch {
    return null;
  }
}

/** Common REST headers for HPC API calls. */
export function hpcHeaders(): Record<string, string> {
  return {
    "X-EMX2-API-Version": API_VERSION,
    "X-Request-Id": crypto.randomUUID(),
    "X-Timestamp": String(Math.floor(Date.now() / 1000)),
  };
}

export function toHex(bytes: Uint8Array): string {
  return Array.from(bytes, (byte) => byte.toString(16).padStart(2, "0")).join(
    ""
  );
}

export async function sha256Hex(blob: Blob): Promise<string> {
  const data = await blob.arrayBuffer();
  const digest = await crypto.subtle.digest("SHA-256", data);
  return toHex(new Uint8Array(digest));
}

// --- Domain re-exports (barrel) ---
export * from "./useJobsApi";
export * from "./useArtifactsApi";
export * from "./useWorkersApi";
