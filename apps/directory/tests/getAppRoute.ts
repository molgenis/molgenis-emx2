export function getAppRoute() {
  return "http://localhost:5173";
  return process.env.E2E_BASE_URL
    ? "directory-demo/directory/"
    : process.env.MOLGENIS_APPS_SCHEMA ?? "";
}
