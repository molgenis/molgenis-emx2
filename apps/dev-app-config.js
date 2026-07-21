const path = process.getBuiltinModule("node:path");
const { createRequire } = process.getBuiltinModule("node:module");
const { pathToFileURL } = process.getBuiltinModule("node:url");

require("./dev-env.js");

const appsDir = __dirname;

const reportPrefix = "molgenis.appconfig=";

const nuxtApps = [
  { name: "catalogue", portKey: "MOLGENIS_PORT_APP_CATALOGUE" },
  { name: "tailwind-components", portKey: "MOLGENIS_PORT_APP_TAILWIND" },
  { name: "ui", portKey: "MOLGENIS_PORT_APP_UI" },
];

const viteApps = [
  { name: "central", portKey: "MOLGENIS_PORT_APP_CENTRAL" },
  { name: "directory", portKey: "MOLGENIS_PORT_APP_DIRECTORY" },
  { name: "emx2-analytics", portKey: null },
  { name: "graphql-playground", portKey: null },
];

const playwrightSuites = [
  { name: "catalogue", configDir: path.join(appsDir, "catalogue") },
  { name: "directory", configDir: path.join(appsDir, "directory") },
  {
    name: "tailwind-components",
    configDir: path.join(appsDir, "tailwind-components"),
  },
  { name: "ui", configDir: path.join(appsDir, "ui") },
  { name: "e2e", configDir: path.join(appsDir, "..", "e2e") },
];

function importFromApp(appDir, specifier) {
  const requireFromApp = createRequire(path.join(appDir, "package.json"));
  return import(pathToFileURL(requireFromApp.resolve(specifier)).href);
}

function nuxtDevServerPort(devServerPort) {
  if (devServerPort === null || devServerPort === undefined) {
    return { port: null, strictPort: false };
  }
  if (typeof devServerPort === "object") {
    return {
      port: devServerPort.port ?? null,
      strictPort:
        devServerPort.random === false &&
        Array.isArray(devServerPort.alternativePortRange) &&
        devServerPort.alternativePortRange.length === 0,
    };
  }
  return { port: Number(devServerPort), strictPort: false };
}

async function resolveNuxtApp(app) {
  const appDir = path.join(appsDir, app.name);
  const { loadNuxtConfig } = await importFromApp(appDir, "nuxt/kit");
  const config = await loadNuxtConfig({ cwd: appDir });
  return {
    ...app,
    kind: "nuxt",
    ...nuxtDevServerPort(config.devServer && config.devServer.port),
    apiBase:
      config.runtimeConfig &&
      config.runtimeConfig.public &&
      config.runtimeConfig.public.apiBase !== undefined
        ? String(config.runtimeConfig.public.apiBase)
        : null,
  };
}

function proxyTargets(proxy) {
  if (!proxy) return [];
  return Object.entries(proxy).map(([pattern, options]) => ({
    pattern,
    target: String(typeof options === "string" ? options : options.target),
  }));
}

async function resolveViteApp(app) {
  const appDir = path.join(appsDir, app.name);
  const vite = await importFromApp(appDir, "vite");
  const config = await vite.resolveConfig(
    { root: appDir },
    "serve",
    "development",
    "development"
  );
  return {
    ...app,
    kind: "vite",
    port: config.server.port === undefined ? null : Number(config.server.port),
    strictPort: config.server.strictPort === true,
    proxyTargets: proxyTargets(config.server.proxy),
  };
}

function playwrightConfigLoader(configDir) {
  const requireFromConfigDir = createRequire(
    path.join(configDir, "package.json")
  );
  const requireFromPlaywrightTest = createRequire(
    requireFromConfigDir.resolve("@playwright/test")
  );
  const playwrightDir = path.dirname(
    requireFromPlaywrightTest.resolve("playwright/package.json")
  );
  return require(path.join(playwrightDir, "lib", "common", "index.js"))
    .configLoader;
}

function firstWebServer(webServer) {
  if (!webServer) return null;
  return Array.isArray(webServer) ? webServer[0] : webServer;
}

async function resolvePlaywrightSuite(suite) {
  const configPath = path.join(suite.configDir, "playwright.config.ts");
  const loaded = await playwrightConfigLoader(
    suite.configDir
  ).loadConfigFromFile(configPath);
  const webServer = firstWebServer(loaded.config.webServer);
  return {
    name: suite.name,
    configPath,
    webServer: webServer
      ? {
          url: webServer.url ?? null,
          envPort:
            webServer.env && webServer.env.PORT
              ? String(webServer.env.PORT)
              : null,
        }
      : null,
  };
}

async function resolve(subject, resolver) {
  try {
    return await resolver(subject);
  } catch (error) {
    return { ...subject, unresolved: error.message };
  }
}

async function main() {
  const apps = [];
  for (const app of nuxtApps) apps.push(await resolve(app, resolveNuxtApp));
  for (const app of viteApps) apps.push(await resolve(app, resolveViteApp));
  const playwright = [];
  for (const suite of playwrightSuites) {
    playwright.push(await resolve(suite, resolvePlaywrightSuite));
  }
  process.stdout.write(
    `${reportPrefix}${JSON.stringify({ apps, playwright })}\n`
  );
}

if (require.main === module) {
  main().catch((error) => {
    console.error(error);
    process.exit(1);
  });
}

module.exports = { reportPrefix };
