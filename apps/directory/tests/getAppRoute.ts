export function getAppRoute() {
  return process.env.MOLGENIS_APPS_HOST
    ? "directory-demo/directory/"
    : process.env.MOLGENIS_APPS_SCHEMA ?? "";
}
