export function getSchemaName() {
 return process.env.E2E_BASE_URL ? 'directory-demo' : process.env.MOLGENIS_APPS_SCHEMA ?? 'directory';
}