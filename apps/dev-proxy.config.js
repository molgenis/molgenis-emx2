const HOST = process.env.MOLGENIS_APPS_HOST || "https://emx2.dev.molgenis.org";
const SCHEMA = process.env.MOLGENIS_APPS_SCHEMA || "pet%20store";

const opts = { changeOrigin: true, secure: false, logLevel: "debug" };

module.exports = {
  "/graphql": {
    target: `${HOST}/${SCHEMA}`,
    ...opts,
  },
  "**/graphql": { target: `${HOST}`, ...opts },
  "/api": { target: `${HOST}`, ...opts },
  "/apps": { target: `${HOST}`, ...opts },
  "/theme.css": { target: `${HOST}/${SCHEMA}`, ...opts },
};
