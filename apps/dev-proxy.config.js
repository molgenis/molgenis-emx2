const HOST = process.env.MOLGENIS_APPS_HOST || "https://emx2.dev.molgenis.org";
const SCHEMA = process.env.MOLGENIS_APPS_SCHEMA || "Catalogue_test";

module.exports = {
  "^/graphql": {
    target: `${HOST}/${SCHEMA}`,
  },
  "/api": { target: `${HOST}` },
  "/apps": { target: `${HOST}` },
  "^/theme.css": { target: `${HOST}/${SCHEMA}` },
};
