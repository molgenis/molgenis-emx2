const fs = process.getBuiltinModule("node:fs");
const path = process.getBuiltinModule("node:path");

const rootEnvPath = path.resolve(__dirname, "..", ".env");
const ambientOverrideKey = "MOLGENIS_ENV_OVERRIDE";

function devPort(key, fallback) {
  const port = Number(process.env[key]);
  return Number.isInteger(port) && port > 0 ? port : fallback;
}

function strictDevServerPort(key, fallback) {
  const declared = devPort(key, null);
  if (declared === null) return fallback;
  return { port: declared, random: false, alternativePortRange: [] };
}

function apiBase(fallback) {
  return process.env.NUXT_PUBLIC_API_BASE || fallback;
}

function appsHost(fallback) {
  return process.env.MOLGENIS_APPS_HOST || fallback;
}

function e2eBaseUrl(portKey, fallback) {
  if (process.env.E2E_BASE_URL) return process.env.E2E_BASE_URL;
  const port = devPort(portKey, null);
  return port === null ? fallback : `http://localhost:${port}/`;
}

function loadRootEnv(envFilePath = rootEnvPath) {
  if (!fs.existsSync(envFilePath)) return {};
  const parsed = parseDotenv(fs.readFileSync(envFilePath, "utf-8"));
  const ambientWins = process.env[ambientOverrideKey] === "1";
  for (const [key, value] of Object.entries(parsed)) {
    const ambient = process.env[key];
    if (ambient === value) continue;
    if (ambient === undefined) {
      process.env[key] = value;
      continue;
    }
    if (ambientWins) {
      announceAmbientKept(key, value, ambient);
      continue;
    }
    process.env[key] = value;
    announceDotenvOverride(key, value, ambient);
  }
  return parsed;
}

function announceDotenvOverride(key, dotenvValue, ambientValue) {
  const dotenv = maskSecret(key, dotenvValue);
  const ambient = maskSecret(key, ambientValue);
  console.warn(
    `[dev-env] ${key}=${dotenv} from .env overrides the ambient ${ambient} — set ${ambientOverrideKey}=1 to keep the ambient value`
  );
}

function announceAmbientKept(key, dotenvValue, ambientValue) {
  const dotenv = maskSecret(key, dotenvValue);
  const ambient = maskSecret(key, ambientValue);
  console.warn(
    `[dev-env] ${ambientOverrideKey}=1: ${key}=${ambient} from the ambient environment overrides the .env value ${dotenv}`
  );
}

function maskSecret(key, value) {
  return /PASS|SECRET|TOKEN/.test(key.toUpperCase()) ? "<HIDDEN>" : value;
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
  devPort,
  strictDevServerPort,
  apiBase,
  appsHost,
  e2eBaseUrl,
  loadRootEnv,
  parseDotenv,
};
