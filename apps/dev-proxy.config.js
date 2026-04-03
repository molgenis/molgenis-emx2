const HOST = process.env.MOLGENIS_APPS_HOST || "https://emx2.dev.molgenis.org";
const SCHEMA = process.env.MOLGENIS_APPS_SCHEMA || "pet store";

const opts = { changeOrigin: true, secure: false, logLevel: "debug" };

module.exports = {
  "/api/graphql": {
    target: `${HOST}/${SCHEMA}`,
    ...opts,
  },
  "^/[a-zA-Z0-9_.%-]+/api/graphql": {
    target: HOST,
    ...opts,
  },
  "^/[a-zA-Z0-9_.%-]+/graphql": {
    target: HOST,
    ...opts,
  },
  "^/[a-zA-Z0-9_.%-]+/api/reports": {
    target: HOST,
    ...opts,
  },
  "^/[a-zA-Z0-9_.%-]+/api/file": {
    target: HOST,
    ...opts,
  },
  "^/[a-zA-Z0-9_.%-]+/api/trigger": {
    target: HOST,
    ...opts,
  },
  "/api": {
    target: `${HOST}/api`,
    ...opts,
  },
  "/graphql": {
    target: `${HOST}/${SCHEMA}/graphql`,
    ...opts,
  },
  "/reports": {
    target: `${HOST}/${SCHEMA}/api/reports`,
    ...opts,
  },
  "/apps": {
    target: HOST,
    ...opts,
  },
  "/theme.css": {
    target: `${HOST}/${SCHEMA}/theme.css`,
    ...opts,
  },
};
