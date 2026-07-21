const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");

const {
  devPort,
  apiBase,
  appsHost,
  e2eBaseUrl,
  loadRootEnv,
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
  "MOLGENIS_JVM_XMX",
  "MOLGENIS_APPS_SCHEMA",
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
  try {
    assertions(envFilePath);
  } finally {
    fs.rmSync(directory, { recursive: true, force: true });
    for (const key of dotenvKeys) {
      if (restore[key] === undefined) delete process.env[key];
      else process.env[key] = restore[key];
    }
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

test("loadRootEnv keeps an existing shell value", () => {
  withEnvFile(
    "MOLGENIS_HTTP_PORT=8083\n",
    { MOLGENIS_HTTP_PORT: "8080" },
    (envFilePath) => {
      loadRootEnv(envFilePath);
      assert.equal(process.env.MOLGENIS_HTTP_PORT, "8080");
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
