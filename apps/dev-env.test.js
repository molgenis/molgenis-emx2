const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");

const {
  devPort,
  strictDevServerPort,
  apiBase,
  appsHost,
  e2eBaseUrl,
  loadRootEnv,
  assertDeclaredBackendConsistent,
  ignoredAppEnvPath,
  warnAboutIgnoredAppEnv,
} = require("./dev-env.js");

const injectedKeys = [
  "E2E_BASE_URL",
  "NUXT_PUBLIC_API_BASE",
  "MOLGENIS_APPS_HOST",
  "MOLGENIS_PORT_TESTAPP",
];

const dotenvKeys = [
  "MOLGENIS_HTTP_PORT",
  "MOLGENIS_POSTGRES_URI",
  "MOLGENIS_POSTGRES_PASS",
  "MOLGENIS_JVM_XMX",
  "MOLGENIS_APPS_SCHEMA",
  "MOLGENIS_ENV_OVERRIDE",
];

function withInjectedEnv(overrides, assertions) {
  const restore = {};
  for (const key of injectedKeys) {
    restore[key] = process.env[key];
    delete process.env[key];
  }
  for (const [key, value] of Object.entries(overrides)) {
    process.env[key] = value;
  }
  try {
    assertions();
  } finally {
    for (const key of injectedKeys) {
      if (restore[key] === undefined) delete process.env[key];
      else process.env[key] = restore[key];
    }
  }
}

function withEnvFile(contents, shellEnv, assertions) {
  const restore = {};
  for (const key of dotenvKeys) {
    restore[key] = process.env[key];
    delete process.env[key];
  }
  for (const [key, value] of Object.entries(shellEnv)) {
    process.env[key] = value;
  }
  const directory = fs.mkdtempSync(path.join(os.tmpdir(), "dev-env-"));
  const envFilePath = path.join(directory, ".env");
  fs.writeFileSync(envFilePath, contents);
  const announcements = [];
  const originalWarn = console.warn;
  console.warn = (line) => announcements.push(line);
  try {
    assertions(envFilePath, announcements);
  } finally {
    console.warn = originalWarn;
    fs.rmSync(directory, { recursive: true, force: true });
    for (const key of dotenvKeys) {
      if (restore[key] === undefined) delete process.env[key];
      else process.env[key] = restore[key];
    }
  }
}

function withAppTree(appEnvContents, assertions) {
  const repoRoot = fs.mkdtempSync(path.join(os.tmpdir(), "dev-env-tree-"));
  const appsPath = path.join(repoRoot, "apps");
  const appPath = path.join(appsPath, "central");
  fs.mkdirSync(appPath, { recursive: true });
  fs.writeFileSync(path.join(repoRoot, ".env"), "MOLGENIS_HTTP_PORT=8083\n");
  if (appEnvContents !== null) {
    fs.writeFileSync(path.join(appPath, ".env"), appEnvContents);
  }
  const announcements = [];
  const originalWarn = console.warn;
  console.warn = (line) => announcements.push(line);
  try {
    assertions({ repoRoot, appsPath, appPath, announcements });
  } finally {
    console.warn = originalWarn;
    fs.rmSync(repoRoot, { recursive: true, force: true });
  }
}

test("devPort returns the declared port as a number", () => {
  withInjectedEnv({ MOLGENIS_PORT_TESTAPP: "3031" }, () => {
    assert.equal(devPort("MOLGENIS_PORT_TESTAPP", 3000), 3031);
  });
});

test("devPort falls back when the key is absent", () => {
  withInjectedEnv({}, () => {
    assert.equal(devPort("MOLGENIS_PORT_TESTAPP", 3000), 3000);
  });
});

test("devPort falls back when the value is not a number", () => {
  withInjectedEnv({ MOLGENIS_PORT_TESTAPP: "not-a-port" }, () => {
    assert.equal(devPort("MOLGENIS_PORT_TESTAPP", 3000), 3000);
  });
});

test("devPort falls back when the value is empty", () => {
  withInjectedEnv({ MOLGENIS_PORT_TESTAPP: "" }, () => {
    assert.equal(devPort("MOLGENIS_PORT_TESTAPP", 3000), 3000);
  });
});

test("devPort reads process.env at call time", () => {
  withInjectedEnv({ MOLGENIS_PORT_TESTAPP: "3031" }, () => {
    assert.equal(devPort("MOLGENIS_PORT_TESTAPP", 3000), 3031);
    process.env.MOLGENIS_PORT_TESTAPP = "3032";
    assert.equal(devPort("MOLGENIS_PORT_TESTAPP", 3000), 3032);
  });
});

test("strictDevServerPort refuses to drift off the declared port", () => {
  withInjectedEnv({ MOLGENIS_PORT_TESTAPP: "3031" }, () => {
    assert.deepEqual(strictDevServerPort("MOLGENIS_PORT_TESTAPP", 3000), {
      port: 3031,
      random: false,
      alternativePortRange: [],
    });
  });
});

test("strictDevServerPort returns the plain fallback when the key is absent", () => {
  withInjectedEnv({}, () => {
    assert.strictEqual(
      strictDevServerPort("MOLGENIS_PORT_TESTAPP", 3000),
      3000
    );
  });
});

test("strictDevServerPort returns the plain fallback when the value is not a number", () => {
  withInjectedEnv({ MOLGENIS_PORT_TESTAPP: "not-a-port" }, () => {
    assert.strictEqual(
      strictDevServerPort("MOLGENIS_PORT_TESTAPP", 3000),
      3000
    );
  });
});

test("apiBase prefers NUXT_PUBLIC_API_BASE over the fallback", () => {
  withInjectedEnv({ NUXT_PUBLIC_API_BASE: "http://localhost:8083/" }, () => {
    assert.equal(
      apiBase("https://emx2.dev.molgenis.org/"),
      "http://localhost:8083/"
    );
  });
});

test("apiBase returns the fallback when the key is absent", () => {
  withInjectedEnv({}, () => {
    assert.equal(
      apiBase("https://emx2.dev.molgenis.org/"),
      "https://emx2.dev.molgenis.org/"
    );
  });
});

test("appsHost prefers MOLGENIS_APPS_HOST over the fallback", () => {
  withInjectedEnv({ MOLGENIS_APPS_HOST: "http://localhost:8083" }, () => {
    assert.equal(
      appsHost("https://emx2.dev.molgenis.org"),
      "http://localhost:8083"
    );
  });
});

test("appsHost returns the fallback when the key is absent", () => {
  withInjectedEnv({}, () => {
    assert.equal(
      appsHost("https://emx2.dev.molgenis.org"),
      "https://emx2.dev.molgenis.org"
    );
  });
});

test("e2eBaseUrl lets E2E_BASE_URL win over the declared port", () => {
  withInjectedEnv(
    {
      E2E_BASE_URL: "https://emx2.dev.molgenis.org/",
      MOLGENIS_PORT_TESTAPP: "3031",
    },
    () => {
      assert.equal(
        e2eBaseUrl("MOLGENIS_PORT_TESTAPP", "http://localhost:3000/"),
        "https://emx2.dev.molgenis.org/"
      );
    }
  );
});

test("e2eBaseUrl uses the declared port when E2E_BASE_URL is not injected", () => {
  withInjectedEnv({ MOLGENIS_PORT_TESTAPP: "3031" }, () => {
    assert.equal(
      e2eBaseUrl("MOLGENIS_PORT_TESTAPP", "http://localhost:3000/"),
      "http://localhost:3031/"
    );
  });
});

test("e2eBaseUrl returns the fallback when neither key is injected", () => {
  withInjectedEnv({}, () => {
    assert.equal(
      e2eBaseUrl("MOLGENIS_PORT_TESTAPP", "http://localhost:3000/"),
      "http://localhost:3000/"
    );
  });
});

test("e2eBaseUrl returns the fallback when the declared port is not a number", () => {
  withInjectedEnv({ MOLGENIS_PORT_TESTAPP: "not-a-port" }, () => {
    assert.equal(
      e2eBaseUrl("MOLGENIS_PORT_TESTAPP", "http://localhost:3000/"),
      "http://localhost:3000/"
    );
  });
});

test("loadRootEnv overrides an existing shell value", () => {
  withEnvFile(
    "MOLGENIS_HTTP_PORT=8083\n",
    { MOLGENIS_HTTP_PORT: "8080" },
    (envFilePath) => {
      loadRootEnv(envFilePath);
      assert.equal(process.env.MOLGENIS_HTTP_PORT, "8083");
    }
  );
});

test("loadRootEnv announces the shell value it overrides", () => {
  withEnvFile(
    "MOLGENIS_HTTP_PORT=8083\n",
    { MOLGENIS_HTTP_PORT: "8080" },
    (envFilePath, announcements) => {
      loadRootEnv(envFilePath);
      assert.equal(announcements.length, 1);
      assert.equal(
        announcements[0],
        "[dev-env] MOLGENIS_HTTP_PORT=8083 from .env overrides the ambient 8080 — set MOLGENIS_ENV_OVERRIDE=1 to keep the ambient value"
      );
    }
  );
});

test("loadRootEnv announces without printing a secret value", () => {
  withEnvFile(
    "MOLGENIS_POSTGRES_PASS=dotenv-secret\n",
    { MOLGENIS_POSTGRES_PASS: "shell-secret" },
    (envFilePath, announcements) => {
      loadRootEnv(envFilePath);
      assert.equal(
        announcements[0],
        "[dev-env] MOLGENIS_POSTGRES_PASS=<HIDDEN> from .env overrides the ambient <HIDDEN> — set MOLGENIS_ENV_OVERRIDE=1 to keep the ambient value"
      );
    }
  );
});

test("loadRootEnv stays silent when the shell already agrees with .env", () => {
  withEnvFile(
    "MOLGENIS_HTTP_PORT=8083\n",
    { MOLGENIS_HTTP_PORT: "8083" },
    (envFilePath, announcements) => {
      loadRootEnv(envFilePath);
      assert.equal(process.env.MOLGENIS_HTTP_PORT, "8083");
      assert.deepEqual(announcements, []);
    }
  );
});

test("loadRootEnv keeps an existing shell value when MOLGENIS_ENV_OVERRIDE=1", () => {
  withEnvFile(
    "MOLGENIS_HTTP_PORT=8083\n",
    { MOLGENIS_HTTP_PORT: "8080", MOLGENIS_ENV_OVERRIDE: "1" },
    (envFilePath) => {
      loadRootEnv(envFilePath);
      assert.equal(process.env.MOLGENIS_HTTP_PORT, "8080");
    }
  );
});

test("loadRootEnv announces the .env value it skips under MOLGENIS_ENV_OVERRIDE=1", () => {
  withEnvFile(
    "MOLGENIS_HTTP_PORT=8083\n",
    { MOLGENIS_HTTP_PORT: "8080", MOLGENIS_ENV_OVERRIDE: "1" },
    (envFilePath, announcements) => {
      loadRootEnv(envFilePath);
      assert.equal(announcements.length, 1);
      assert.equal(
        announcements[0],
        "[dev-env] MOLGENIS_ENV_OVERRIDE=1: MOLGENIS_HTTP_PORT=8080 from the ambient environment overrides the .env value 8083"
      );
    }
  );
});

test("loadRootEnv still fills an unset key when MOLGENIS_ENV_OVERRIDE=1", () => {
  withEnvFile(
    "MOLGENIS_HTTP_PORT=8083\n",
    { MOLGENIS_ENV_OVERRIDE: "1" },
    (envFilePath, announcements) => {
      loadRootEnv(envFilePath);
      assert.equal(process.env.MOLGENIS_HTTP_PORT, "8083");
      assert.deepEqual(announcements, []);
    }
  );
});

test("loadRootEnv fills a key the shell does not set", () => {
  withEnvFile("MOLGENIS_HTTP_PORT=8083\n", {}, (envFilePath) => {
    loadRootEnv(envFilePath);
    assert.equal(process.env.MOLGENIS_HTTP_PORT, "8083");
  });
});

test("loadRootEnv ignores blank lines and comments", () => {
  withEnvFile("\n# a comment\nMOLGENIS_HTTP_PORT=8083\n", {}, (envFilePath) => {
    assert.deepEqual(loadRootEnv(envFilePath), { MOLGENIS_HTTP_PORT: "8083" });
  });
});

test("loadRootEnv keeps everything after the first equals sign", () => {
  withEnvFile(
    "MOLGENIS_POSTGRES_URI=jdbc:postgresql://localhost:5434/molgenis?a=b",
    {},
    (envFilePath) => {
      assert.equal(
        loadRootEnv(envFilePath).MOLGENIS_POSTGRES_URI,
        "jdbc:postgresql://localhost:5434/molgenis?a=b"
      );
    }
  );
});

test("assertDeclaredBackendConsistent refuses a loopback target on another port", () => {
  assert.throws(
    () =>
      assertDeclaredBackendConsistent({
        MOLGENIS_HTTP_PORT: "8083",
        MOLGENIS_APPS_HOST: "http://localhost:8084",
      }),
    (error) =>
      error.message.includes("MOLGENIS_HTTP_PORT=8083") &&
      error.message.includes("MOLGENIS_APPS_HOST=http://localhost:8084")
  );
});

test("assertDeclaredBackendConsistent refuses a 127.0.0.1 apiBase on another port", () => {
  assert.throws(
    () =>
      assertDeclaredBackendConsistent({
        MOLGENIS_HTTP_PORT: "8083",
        NUXT_PUBLIC_API_BASE: "http://127.0.0.1:8080/",
      }),
    (error) =>
      error.message.includes("NUXT_PUBLIC_API_BASE=http://127.0.0.1:8080/")
  );
});

test("assertDeclaredBackendConsistent accepts loopback targets on the declared port", () => {
  assert.doesNotThrow(() =>
    assertDeclaredBackendConsistent({
      MOLGENIS_HTTP_PORT: "8083",
      MOLGENIS_APPS_HOST: "http://localhost:8083",
      NUXT_PUBLIC_API_BASE: "http://localhost:8083/",
    })
  );
});

test("assertDeclaredBackendConsistent ignores a remote target whatever the declared port", () => {
  assert.doesNotThrow(() =>
    assertDeclaredBackendConsistent({
      MOLGENIS_HTTP_PORT: "8083",
      MOLGENIS_APPS_HOST: "https://emx2.dev.molgenis.org",
      NUXT_PUBLIC_API_BASE: "https://emx2.dev.molgenis.org/",
    })
  );
});

test("assertDeclaredBackendConsistent skips when the backend port is not declared", () => {
  assert.doesNotThrow(() =>
    assertDeclaredBackendConsistent({
      NUXT_PUBLIC_API_BASE: "http://localhost:8084/",
    })
  );
});

test("assertDeclaredBackendConsistent skips when no target is declared", () => {
  assert.doesNotThrow(() =>
    assertDeclaredBackendConsistent({ MOLGENIS_HTTP_PORT: "8083" })
  );
});

test("loadRootEnv strips matching surrounding quotes", () => {
  withEnvFile(
    "MOLGENIS_JVM_XMX=\"1g\"\nMOLGENIS_APPS_SCHEMA='pet store'",
    {},
    (envFilePath) => {
      assert.deepEqual(loadRootEnv(envFilePath), {
        MOLGENIS_JVM_XMX: "1g",
        MOLGENIS_APPS_SCHEMA: "pet store",
      });
    }
  );
});

test("ignoredAppEnvPath finds the .env beside the config that loaded the module", () => {
  withAppTree("MOLGENIS_HTTP_PORT=9099\n", ({ appsPath, appPath }) => {
    assert.equal(
      ignoredAppEnvPath(appPath, appsPath),
      path.join(appPath, ".env")
    );
  });
});

test("ignoredAppEnvPath finds nothing when the app declares no .env", () => {
  withAppTree(null, ({ appsPath, appPath }) => {
    assert.equal(ignoredAppEnvPath(appPath, appsPath), null);
  });
});

test("ignoredAppEnvPath ignores the repo-root .env itself", () => {
  withAppTree(null, ({ repoRoot, appsPath }) => {
    assert.equal(ignoredAppEnvPath(repoRoot, appsPath), null);
  });
});

test("ignoredAppEnvPath ignores a working directory outside apps", () => {
  withAppTree("MOLGENIS_HTTP_PORT=9099\n", ({ appsPath, appPath }) => {
    assert.equal(
      ignoredAppEnvPath(path.join(appPath, "nested"), appsPath),
      null
    );
    assert.equal(ignoredAppEnvPath(appsPath, appsPath), null);
  });
});

test("warnAboutIgnoredAppEnv names the file once and says what still works", () => {
  withAppTree(
    "MOLGENIS_HTTP_PORT=9099\n",
    ({ appsPath, appPath, announcements }) => {
      warnAboutIgnoredAppEnv(appPath, appsPath);
      assert.equal(announcements.length, 1);
      assert.equal(
        announcements[0],
        `[dev-env] ${path.join(
          appPath,
          ".env"
        )} is ignored for stack configuration — MOLGENIS_* and NUXT_PUBLIC_* keys belong in the repo-root .env; VITE_* keys in it still reach import.meta.env through Vite's own loading`
      );
    }
  );
});

test("warnAboutIgnoredAppEnv stays silent when the app declares no .env", () => {
  withAppTree(null, ({ appsPath, appPath, announcements }) => {
    warnAboutIgnoredAppEnv(appPath, appsPath);
    assert.deepEqual(announcements, []);
  });
});

test("warnAboutIgnoredAppEnv stays silent for the repo-root .env itself", () => {
  withAppTree(null, ({ repoRoot, appsPath, announcements }) => {
    warnAboutIgnoredAppEnv(repoRoot, appsPath);
    assert.deepEqual(announcements, []);
  });
});
