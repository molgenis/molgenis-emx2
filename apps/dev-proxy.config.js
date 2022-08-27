const HOST = process.env.MOLGENIS_APPS_HOST || "http://localhost:8080";
const SCHEMA = process.env.MOLGENIS_APPS_SCHEMA || "fairgenomes";

const opts = { changeOrigin: true, secure: false, logLevel: "debug" };

module.exports = {
  "/graphql": {
    target: `${HOST}/${SCHEMA}`,
    ...opts,
  },
  "^/[a-zA-Z0-9_.-]+/graphql": {
    target: `${HOST}`,
    ...opts,
  },
  "/api": { target: `${HOST}`, ...opts },
  "/apps": { target: `${HOST}`, ...opts },
  "/theme.css": { target: `${HOST}/${SCHEMA}`, ...opts },
};
