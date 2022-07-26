const HOST = process.env.MOLGENIS_APPS_HOST || "http://127.0.0.1:8080";
const SCHEMA = process.env.MOLGENIS_APPS_SCHEMA || "pet%20store";

const opts = { changeOrigin: true, secure: false, logLevel: 'debug' };

module.exports = {
  "/graphql": {
    target: `${HOST}/${SCHEMA}`,
    ...opts,
  },
  "**/graphql": { target: `${HOST}`, ...opts },
  "/api": { target: `${HOST}`, ...opts },
  "/apps": { target: `${HOST}`, ...opts },
  "/theme.css": { target: `${HOST}/apps/central/theme.css`, ...opts },
};
