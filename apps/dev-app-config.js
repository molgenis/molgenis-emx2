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
  { name: "emx2-analytics", portKey: null },
  { name: "graphql-playground", portKey: null },
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

async function resolveApp(app, resolver) {
  try {
    return await resolver(app);
  } catch (error) {
    return { ...app, error: error.message };
  }
}

async function main() {
  const resolved = [];
  for (const app of nuxtApps)
    resolved.push(await resolveApp(app, resolveNuxtApp));
  for (const app of viteApps)
    resolved.push(await resolveApp(app, resolveViteApp));
  process.stdout.write(`${reportPrefix}${JSON.stringify(resolved)}\n`);
}

if (require.main === module) {
  main().catch((error) => {
    console.error(error);
    process.exit(1);
  });
}

module.exports = { reportPrefix };
