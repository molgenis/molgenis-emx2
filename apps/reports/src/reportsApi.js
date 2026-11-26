export function reportsApiUrl(format, query) {
  // the first path segment is the schema id and is already URL-encoded
  const schemaPath = window.location.pathname.split("/")[1];
  // without a schema segment, keep the old relative path
  const base = schemaPath ? `/${schemaPath}/api/reports` : "../api/reports";
  return `${base}/${format}?${query}`;
}
