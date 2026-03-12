import {
  gqlString,
  isSchemaNotReady,
  gqlQuery,
  hpcHeaders,
  toHex,
  REST_BASE,
} from "./useHpcApi";

interface FetchArtifactsOpts {
  status?: string;
  limit?: number;
  offset?: number;
}

type ArtifactRelation = { name?: string | null };

type ArtifactGraphqlRow = {
  id: string;
  name?: string | null;
  type?: string | null;
  residence?: string | ArtifactRelation | null;
  status?: string | ArtifactRelation | null;
  sha256?: string | null;
  size_bytes?: number | string | null;
  content_url?: string | null;
  metadata?: unknown;
  created_at?: string | null;
  committed_at?: string | null;
};

export type ArtifactFileRow = {
  id: string;
  path: string;
  sha256?: string | null;
  size_bytes?: number | string | null;
  content_type?: string | null;
};

export type ArtifactSummary = {
  id: string;
  name?: string | null;
  type?: string | null;
  status?: string | null;
  residence?: string | null;
};

type FetchArtifactsResponse = {
  HpcArtifacts?: ArtifactGraphqlRow[];
  HpcArtifacts_agg?: { count?: number };
};

type FetchArtifactDetailResponse = {
  HpcArtifacts?: ArtifactGraphqlRow[];
  HpcArtifactFiles?: ArtifactFileRow[];
};

type RestArtifactResponse = ArtifactSummary & {
  _links?: Record<string, string>;
};

type ArtifactCommitResponse = RestArtifactResponse & {
  sha256?: string | null;
  size_bytes?: number | string | null;
};

type UploadResponse = { sha256: string };

export type NormalizedArtifact = {
  id: string;
  name?: string | null;
  type?: string | null;
  residence?: string | null;
  status?: string | null;
  sha256?: string | null;
  size_bytes?: number | string | null;
  content_url?: string | null;
  metadata?: unknown;
  created_at?: string | null;
  committed_at?: string | null;
  [key: string]: unknown;
};

/** Progress info emitted during a file upload. */
export interface UploadProgress {
  loaded: number;
  total: number;
}

/** Handle returned by uploadArtifactFileXhr — allows cancellation. */
export interface UploadHandle {
  /** Resolves when the upload (and server response) completes. */
  promise: Promise<{ sha256: string }>;
  /** Abort the in-flight upload. */
  abort: () => void;
}

const SHA256_CHUNK_SIZE = 1024 * 1024; // 1 MB

const artifactSummaryCache = new Map<string, ArtifactSummary | null>();

/**
 * Fetch artifacts via GraphQL.
 */
export async function fetchArtifacts({
  status,
  limit = 50,
  offset = 0,
}: FetchArtifactsOpts = {}): Promise<{
  items: NormalizedArtifact[];
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
      ${filterClause ? `${filterClause},` : ""}
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
    const data = await gqlQuery<FetchArtifactsResponse>(query);
    return {
      items: (data.HpcArtifacts || []).map(normalizeArtifact),
      totalCount: data.HpcArtifacts_agg?.count ?? 0,
    };
  } catch (err: unknown) {
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
  artifact: NormalizedArtifact | null;
  files: ArtifactFileRow[];
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
    const data = await gqlQuery<FetchArtifactDetailResponse>(query);
    const artifact = data.HpcArtifacts?.[0];
    return {
      artifact: artifact ? normalizeArtifact(artifact) : null,
      files: data.HpcArtifactFiles || [],
    };
  } catch (err: unknown) {
    if (isSchemaNotReady(err)) {
      return { artifact: null, files: [], schemaNotReady: true };
    }
    throw err;
  }
}

export async function fetchArtifactSummary(
  id: string
): Promise<ArtifactSummary | null> {
  if (artifactSummaryCache.has(id)) {
    return artifactSummaryCache.get(id) ?? null;
  }

  try {
    const artifact = await $fetch<RestArtifactResponse>(
      `${REST_BASE}/artifacts/${id}`,
      {
      method: "GET",
      headers: hpcHeaders(),
      }
    );
    const summary = {
      id,
      name: artifact?.name ?? null,
      type: artifact?.type ?? null,
      status: artifact?.status ?? null,
      residence: artifact?.residence ?? null,
    };
    artifactSummaryCache.set(id, summary);
    return summary;
  } catch {
    artifactSummaryCache.set(id, null);
    return null;
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
} = {}): Promise<RestArtifactResponse> {
  const body: Record<string, string> = { type, residence };
  if (name) body.name = name;
  if (content_url) body.content_url = content_url;
  return await $fetch<RestArtifactResponse>(`${REST_BASE}/artifacts`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...hpcHeaders(),
    },
    body,
  });
}

/**
 * Upload a file to an artifact via PUT (legacy $fetch — no progress).
 * @deprecated Use {@link uploadArtifactFileXhr} for progress + cancellation.
 */
export async function uploadArtifactFile(
  artifactId: string,
  file: File,
  path?: string
): Promise<UploadResponse> {
  const filePath = path || file.name;
  const sha256 = await streamingSha256(file);
  return await $fetch<UploadResponse>(
    `${REST_BASE}/artifacts/${artifactId}/files/${filePath}`,
    {
      method: "PUT",
      headers: {
        "Content-Type": file.type || "application/octet-stream",
        "Content-SHA256": sha256,
        ...hpcHeaders(),
      },
      body: file,
    }
  );
}

/**
 * Compute SHA-256 of a File by reading it in 1 MB chunks.
 *
 * SubtleCrypto.digest does not support incremental updates, so chunks are
 * collected and concatenated before a single digest call. This still avoids
 * the previous double-read problem (sha256Hex + upload body) because the
 * XHR body reads the disk-backed File blob separately.
 */
export async function streamingSha256(file: File): Promise<string> {
  const size = file.size;
  const chunks: ArrayBuffer[] = [];
  let offset = 0;

  while (offset < size) {
    const end = Math.min(offset + SHA256_CHUNK_SIZE, size);
    chunks.push(await file.slice(offset, end).arrayBuffer());
    offset = end;
  }

  const totalLength = chunks.reduce((sum, buf) => sum + buf.byteLength, 0);
  const combined = new Uint8Array(totalLength);
  let pos = 0;
  for (const buf of chunks) {
    combined.set(new Uint8Array(buf), pos);
    pos += buf.byteLength;
  }

  const digest = await crypto.subtle.digest("SHA-256", combined);
  return toHex(new Uint8Array(digest));
}

/**
 * Upload a file to an artifact via XMLHttpRequest for upload progress tracking.
 * Computes Content-SHA256 in a streaming pass before uploading.
 *
 * @returns An UploadHandle with a promise (resolves with the per-file sha256) and an abort function.
 */
export function uploadArtifactFileXhr(
  artifactId: string,
  file: File,
  filePath: string,
  onProgress?: (progress: UploadProgress) => void
): UploadHandle {
  let aborted = false;
  let xhr: XMLHttpRequest | null = null;

  const promise = (async () => {
    // 1. Compute SHA-256 before upload (needed for Content-SHA256 header)
    const sha256 = await streamingSha256(file);
    if (aborted) throw new DOMException("Upload aborted", "AbortError");

    // 2. Upload via XHR for progress events
    return new Promise<{ sha256: string }>((resolve, reject) => {
      xhr = new XMLHttpRequest();
      const url = `${REST_BASE}/artifacts/${artifactId}/files/${filePath}`;
      xhr.open("PUT", url);

      // Set headers
      const headers: Record<string, string> = {
        "Content-Type": file.type || "application/octet-stream",
        "Content-SHA256": sha256,
        ...hpcHeaders(),
      };
      for (const [key, value] of Object.entries(headers)) {
        xhr.setRequestHeader(key, value);
      }

      xhr.upload.onprogress = (event) => {
        if (event.lengthComputable && onProgress) {
          onProgress({ loaded: event.loaded, total: event.total });
        }
      };

      xhr.onload = () => {
        if (!xhr) {
          reject(new Error("Upload failed: request missing"));
          return;
        }

        if (xhr.status >= 200 && xhr.status < 300) {
          resolve({ sha256 });
        } else {
          let msg = `Upload failed: ${xhr.status}`;
          try {
            const body = JSON.parse(xhr.responseText) as {
              errors?: Array<{ message?: string }>;
              message?: string;
            };
            if (body?.errors?.[0]?.message) msg = body.errors[0].message;
            else if (body?.message) msg = body.message;
          } catch {
            if (xhr.responseText) msg += ` ${xhr.responseText}`;
          }
          reject(new Error(msg));
        }
      };

      xhr.onerror = () => reject(new Error("Network error during upload"));
      xhr.onabort = () =>
        reject(new DOMException("Upload aborted", "AbortError"));

      xhr.send(file);
    });
  })();

  return {
    promise,
    abort: () => {
      aborted = true;
      xhr?.abort();
    },
  };
}

/**
 * Upload a single file with retry logic (up to maxAttempts with exponential backoff).
 */
export function uploadArtifactFileWithRetry(
  artifactId: string,
  file: File,
  filePath: string,
  onProgress?: (progress: UploadProgress) => void,
  maxAttempts: number = 3
): UploadHandle {
  let currentHandle: UploadHandle | null = null;
  let aborted = false;

  const promise = (async () => {
    let lastError: Error | null = null;
    for (let attempt = 1; attempt <= maxAttempts; attempt++) {
      if (aborted) throw new DOMException("Upload aborted", "AbortError");

      currentHandle = uploadArtifactFileXhr(
        artifactId,
        file,
        filePath,
        onProgress
      );

      try {
        return await currentHandle.promise;
      } catch (err: unknown) {
        lastError = err instanceof Error ? err : new Error(String(err));
        // Don't retry on abort
        if (err instanceof DOMException && err.name === "AbortError") throw err;
        // Don't retry on 4xx client errors (except 408/429)
        const message = err instanceof Error ? err.message : "";
        if (
          message.match(/Upload failed: 4\d\d/) &&
          !message.includes("408") &&
          !message.includes("429")
        ) {
          throw err;
        }
        if (attempt < maxAttempts) {
          // Exponential backoff: 1s, 2s, 4s ...
          const delay = 2 ** (attempt - 1) * 1000;
          await new Promise((r) => setTimeout(r, delay));
        }
      }
    }
    throw lastError ?? new Error("Upload failed after retries");
  })();

  return {
    promise,
    abort: () => {
      aborted = true;
      currentHandle?.abort();
    },
  };
}

/**
 * Compute the tree hash from per-file SHA-256 hex digests.
 * Matches the server-side algorithm in ArtifactService.java:
 * - Single file: return the file's own sha256
 * - Multiple files: sort by path, concatenate "path:sha256" strings (no separator),
 *   SHA-256 the result
 */
export async function computeTreeHash(
  fileHashes: Array<{ path: string; sha256: string }>
): Promise<string> {
  if (fileHashes.length === 0) {
    throw new Error("Cannot compute tree hash of empty file list");
  }

  const sorted = [...fileHashes].sort((a, b) => a.path.localeCompare(b.path));

  if (sorted.length === 1) {
    return sorted[0].sha256;
  }

  // Multi-file: tree hash over sorted path:sha256 pairs
  const canonical = sorted.map((f) => `${f.path}:${f.sha256}`).join("");
  const encoded = new TextEncoder().encode(canonical);
  const digest = await crypto.subtle.digest("SHA-256", encoded);
  return toHex(new Uint8Array(digest));
}

/**
 * Commit an artifact with final hash and size.
 */
export async function commitArtifact(
  artifactId: string,
  { sha256, size_bytes }: { sha256?: string; size_bytes?: number } = {}
): Promise<ArtifactCommitResponse> {
  const body: { sha256?: string; size_bytes?: number } = {};
  if (sha256) body.sha256 = sha256;
  if (size_bytes != null) body.size_bytes = size_bytes;
  return await $fetch<ArtifactCommitResponse>(
    `${REST_BASE}/artifacts/${artifactId}/commit`,
    {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...hpcHeaders(),
    },
    body,
    }
  );
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

/** Flatten REF/ONTOLOGY objects for artifacts and parse metadata JSON. */
export function normalizeArtifact(
  artifact: ArtifactGraphqlRow
): NormalizedArtifact {
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
