import {
  gqlString,
  isSchemaNotReady,
  gqlQuery,
  hpcHeaders,
  sha256Hex,
  REST_BASE,
} from "./useHpcApi";

interface FetchArtifactsOpts {
  status?: string;
  limit?: number;
  offset?: number;
}

const artifactSummaryCache = new Map<
  string,
  {
    id: string;
    name?: string | null;
    type?: string | null;
    status?: string | null;
    residence?: string | null;
  } | null
>();

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

export async function fetchArtifactSummary(id: string): Promise<{
  id: string;
  name?: string | null;
  type?: string | null;
  status?: string | null;
  residence?: string | null;
} | null> {
  if (artifactSummaryCache.has(id)) {
    return artifactSummaryCache.get(id) ?? null;
  }

  try {
    const artifact = await $fetch<any>(`${REST_BASE}/artifacts/${id}`, {
      method: "GET",
      headers: hpcHeaders(),
    });
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

/** Flatten REF/ONTOLOGY objects for artifacts and parse metadata JSON. */
export function normalizeArtifact(artifact: any): any {
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
