const fs = process.getBuiltinModule("node:fs");
const path = process.getBuiltinModule("node:path");

const rootEnvPath = path.resolve(__dirname, "..", ".env");

function apiBase(fallback) {
  return process.env.NUXT_PUBLIC_API_BASE || fallback;
}

function appsHost(fallback) {
  return process.env.MOLGENIS_APPS_HOST || fallback;
}

function loadRootEnv(envFilePath = rootEnvPath) {
  if (!fs.existsSync(envFilePath)) return {};
  const parsed = parseDotenv(fs.readFileSync(envFilePath, "utf-8"));
  for (const [key, value] of Object.entries(parsed)) {
    process.env[key] = value;
  }
  applyDeclaredBackendTarget(parsed);
  return parsed;
}

function applyDeclaredBackendTarget(declared) {
  const declaredPort = Number(declared.MOLGENIS_HTTP_PORT);
  if (!Number.isInteger(declaredPort) || declaredPort <= 0) return;
  const declaredBackendUrl = `http://localhost:${declaredPort}`;
  for (const key of ["NUXT_PUBLIC_API_BASE", "MOLGENIS_APPS_HOST"]) {
    if (!declared[key]) process.env[key] = declaredBackendUrl;
  }
}

function parseDotenv(contents) {
  const parsed = {};
  for (const line of contents.split("\n")) {
    const trimmed = line.trim();
    if (trimmed === "" || trimmed.startsWith("#")) continue;
    const separator = trimmed.indexOf("=");
    if (separator < 0) continue;
    const key = trimmed.slice(0, separator).trim();
    parsed[key] = stripSurroundingQuotes(trimmed.slice(separator + 1).trim());
  }
  return parsed;
}

function stripSurroundingQuotes(value) {
  const quoted =
    value.length >= 2 &&
    ((value.startsWith('"') && value.endsWith('"')) ||
      (value.startsWith("'") && value.endsWith("'")));
  return quoted ? value.slice(1, -1) : value;
}

loadRootEnv();

module.exports = {
  apiBase,
  appsHost,
  loadRootEnv,
  parseDotenv,
};
