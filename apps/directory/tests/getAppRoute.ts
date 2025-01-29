export function getAppRoute() {
  return process.env.E2E_BASE_URL
    ? "directory-demo/directory/"
    : process.env.MOLGENIS_APPS_SCHEMA ?? "";
}
