const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const { execFileSync } = require("node:child_process");

const { parseDotenv } = require("./dev-env.js");

const appsDir = __dirname;
const turboBin = path.join(appsDir, "node_modules", ".bin", "turbo");
const rootEnvPath = path.join(appsDir, "..", ".env");

const bakingApps = ["tailwind-components", "ui"];

function turboDryRun(packageName, backendEnv = {}) {
  const stdout = execFileSync(
    turboBin,
    ["run", "build", `--filter=${packageName}`, "--dry=json"],
    {
      cwd: appsDir,
      encoding: "utf-8",
      maxBuffer: 64 * 1024 * 1024,
      stdio: ["ignore", "pipe", "ignore"],
      env: { ...process.env, ...backendEnv },
    }
  );
  return JSON.parse(stdout);
}

function buildHash(packageName, backendEnv) {
  const task = turboDryRun(packageName, backendEnv).tasks.find(
    (candidate) => candidate.package === packageName
  );
  assert.ok(task, `turbo reported no build task for ${packageName}`);
  return task.hash;
}

function globalCacheFiles() {
  return Object.keys(
    turboDryRun("tailwind-components").globalCacheInputs.files
  );
}

function declaredEnv() {
  if (!fs.existsSync(rootEnvPath)) return {};
  return parseDotenv(fs.readFileSync(rootEnvPath, "utf-8"));
}

function bakedApiBases(appName) {
  const indexHtml = path.join(appsDir, appName, "dist", "index.html");
  if (!fs.existsSync(indexHtml)) return null;
  const contents = fs.readFileSync(indexHtml, "utf-8");
  const matches = [...contents.matchAll(/apiBase:"([^"]*)"/g)].map(
    (match) => match[1]
  );
  assert.ok(
    matches.length > 0,
    `${indexHtml} bakes no recognisable apiBase; the Nuxt output shape changed and this check needs updating`
  );
  return [...new Set(matches)];
}

test("a different NUXT_PUBLIC_API_BASE gives a Nuxt app a different build hash", () => {
  assert.notEqual(
    buildHash("tailwind-components", {
      NUXT_PUBLIC_API_BASE: "http://localhost:8083/",
    }),
    buildHash("tailwind-components", {
      NUXT_PUBLIC_API_BASE: "http://localhost:9099/",
    })
  );
});

test("a different NUXT_PUBLIC_API_BASE gives a Vite app a different build hash", () => {
  assert.notEqual(
    buildHash("central", { NUXT_PUBLIC_API_BASE: "http://localhost:8083/" }),
    buildHash("central", { NUXT_PUBLIC_API_BASE: "http://localhost:9099/" })
  );
});

test("a different MOLGENIS_APPS_HOST gives a build a different hash", () => {
  assert.notEqual(
    buildHash("central", { MOLGENIS_APPS_HOST: "http://localhost:8083" }),
    buildHash("central", { MOLGENIS_APPS_HOST: "http://localhost:9099" })
  );
});

test("the shared resolvers are part of every build's cache key", () => {
  const files = globalCacheFiles();
  assert.ok(
    files.includes("dev-env.js") && files.includes("dev-proxy.config.js"),
    `turbo hashes ${JSON.stringify(
      files
    )}, so a changed resolver would replay a stale bundle`
  );
});

test("built bundles bake the backend the repo-root .env declares", (t) => {
  const declared = declaredEnv().NUXT_PUBLIC_API_BASE;
  if (!declared) {
    return t.skip("the repo-root .env declares no NUXT_PUBLIC_API_BASE");
  }
  const built = bakingApps.filter((appName) => bakedApiBases(appName) !== null);
  if (built.length === 0) return t.skip("no app has been built yet");
  for (const appName of built) {
    assert.deepEqual(
      bakedApiBases(appName),
      [declared],
      `${appName}/dist is baked against a backend the repo-root .env does not declare`
    );
  }
});
