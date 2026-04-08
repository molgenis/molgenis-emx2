import { navigateTo } from "#app/composables/router";
import { API_VERSION } from "../utils/protocol";

export const GRAPHQL_URL = "/_SYSTEM_/graphql";
export const REST_BASE = "/api/hpc";
export const ACTIVE_JOB_STATUSES = new Set(["CLAIMED", "SUBMITTED", "STARTED"]);

type GraphqlErrorEntry = { message?: string };
type GraphqlFetchError = {
  message?: string;
  status?: number;
  statusCode?: number;
  data?: { errors?: GraphqlErrorEntry[] };
  response?: { errors?: GraphqlErrorEntry[] };
};

type GraphqlResponse<T> = {
  data?: T;
  errors?: GraphqlErrorEntry[];
};

/** Safely encode a JS string as a GraphQL string literal. */
export function gqlString(value: string): string {
  return JSON.stringify(String(value));
}

/**
 * Returns true if the error indicates HPC tables don't exist yet
 * (e.g. FieldUndefined for HpcJobs, HpcWorkers, etc.).
 */
export function isSchemaNotReady(err: unknown): boolean {
  const errorLike = (err ?? {}) as GraphqlFetchError;
  const msg =
    errorLike.response?.errors?.[0]?.message ?? errorLike.message ?? "";
  return msg.includes("FieldUndefined") || msg.includes("is undefined");
}

/**
 * Returns true if the error indicates the user's session has expired or they
 * lack permission to access the _SYSTEM_ schema.
 */
function isAuthError(err: unknown): boolean {
  const errorLike = (err ?? {}) as GraphqlFetchError;
  const msg =
    errorLike.data?.errors?.[0]?.message ??
    errorLike.message ??
    String(err ?? "");
  return (
    msg.includes("sign in") ||
    msg.includes("unknown") ||
    errorLike.status === 403 ||
    errorLike.statusCode === 403
  );
}

/** Execute a GraphQL query against the _SYSTEM_ schema. */
export async function gqlQuery<T = Record<string, unknown>>(
  query: string
): Promise<T> {
  let resp: GraphqlResponse<T>;
  try {
    resp = await $fetch<GraphqlResponse<T>>(GRAPHQL_URL, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: { query },
    });
  } catch (fetchErr: unknown) {
    if (isAuthError(fetchErr)) {
      await navigateTo("/login");
      throw fetchErr;
    }
    throw fetchErr;
  }
  if (resp.errors?.length) {
    const err = new Error(resp.errors[0].message);
    if (isAuthError({ message: resp.errors[0].message })) {
      await navigateTo("/login");
      throw err;
    }
    throw err;
  }
  return resp.data ?? (resp as T);
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

export * from "./useArtifactsApi";
// --- Domain re-exports (barrel) ---
export * from "./useJobsApi";
export * from "./useWorkersApi";
