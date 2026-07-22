const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");

const { apiBase, appsHost, loadRootEnv } = require("./dev-env.js");

const injectedKeys = [
  "NUXT_PUBLIC_API_BASE",
  "MOLGENIS_APPS_HOST",
  "MOLGENIS_HTTP_PORT",
];

function withEnv(ambientEnv, assertions) {
  const restore = {};
  for (const key of injectedKeys) {
    restore[key] = process.env[key];
    delete process.env[key];
  }
  for (const [key, value] of Object.entries(ambientEnv)) {
    process.env[key] = value;
  }
  const directory = fs.mkdtempSync(path.join(os.tmpdir(), "dev-env-"));
  try {
    assertions(path.join(directory, ".env"));
  } finally {
    fs.rmSync(directory, { recursive: true, force: true });
    for (const key of injectedKeys) {
      if (restore[key] === undefined) delete process.env[key];
      else process.env[key] = restore[key];
    }
  }
}

function declareStack(envFilePath, contents) {
  fs.writeFileSync(envFilePath, contents);
  loadRootEnv(envFilePath);
}

test("a declared MOLGENIS_HTTP_PORT outranks an ambient NUXT_PUBLIC_API_BASE", () => {
  withEnv({ NUXT_PUBLIC_API_BASE: "http://localhost:8080/" }, (envFilePath) => {
    declareStack(envFilePath, "MOLGENIS_HTTP_PORT=8083\n");
    assert.equal(
      apiBase("https://emx2.dev.molgenis.org/"),
      "http://localhost:8083"
    );
  });
});

test("a declared MOLGENIS_HTTP_PORT outranks an ambient MOLGENIS_APPS_HOST", () => {
  withEnv({ MOLGENIS_APPS_HOST: "http://localhost:8080" }, (envFilePath) => {
    declareStack(envFilePath, "MOLGENIS_HTTP_PORT=8083\n");
    assert.equal(
      appsHost("https://emx2.dev.molgenis.org"),
      "http://localhost:8083"
    );
  });
});

test("an explicit NUXT_PUBLIC_API_BASE in .env outranks the declared port", () => {
  withEnv({ NUXT_PUBLIC_API_BASE: "http://localhost:8080/" }, (envFilePath) => {
    declareStack(
      envFilePath,
      "MOLGENIS_HTTP_PORT=8083\nNUXT_PUBLIC_API_BASE=http://localhost:8084/\n"
    );
    assert.equal(
      apiBase("https://emx2.dev.molgenis.org/"),
      "http://localhost:8084/"
    );
  });
});

test("an explicit MOLGENIS_APPS_HOST in .env outranks the declared port", () => {
  withEnv({ MOLGENIS_APPS_HOST: "http://localhost:8080" }, (envFilePath) => {
    declareStack(
      envFilePath,
      "MOLGENIS_HTTP_PORT=8083\nMOLGENIS_APPS_HOST=http://localhost:8084\n"
    );
    assert.equal(
      appsHost("https://emx2.dev.molgenis.org"),
      "http://localhost:8084"
    );
  });
});

test("without a .env file the ambient environment is left untouched", () => {
  withEnv(
    {
      NUXT_PUBLIC_API_BASE: "http://localhost:8080/",
      MOLGENIS_APPS_HOST: "http://localhost:8080",
    },
    (envFilePath) => {
      assert.deepEqual(loadRootEnv(envFilePath), {});
      assert.equal(
        apiBase("https://emx2.dev.molgenis.org/"),
        "http://localhost:8080/"
      );
      assert.equal(
        appsHost("https://emx2.dev.molgenis.org"),
        "http://localhost:8080"
      );
    }
  );
});

test("an ambient NUXT_PUBLIC_API_BASE outranks the caller's fallback", () => {
  withEnv({ NUXT_PUBLIC_API_BASE: "http://localhost:8080/" }, () => {
    assert.equal(
      apiBase("https://emx2.dev.molgenis.org/"),
      "http://localhost:8080/"
    );
  });
});

test("an ambient MOLGENIS_APPS_HOST outranks the caller's fallback", () => {
  withEnv({ MOLGENIS_APPS_HOST: "http://localhost:8080" }, () => {
    assert.equal(
      appsHost("https://emx2.dev.molgenis.org"),
      "http://localhost:8080"
    );
  });
});
