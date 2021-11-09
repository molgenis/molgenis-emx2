const HOST = process.env.MOLGENIS_APPS_HOST || "http://localhost:8080";
const SCHEMA = process.env.MOLGENIS_APPS_SCHEMA || "testNamesWithSpaces";

module.exports = {
  "^/graphql": {
    target: `${HOST}/${SCHEMA}`,
  },
  "/api": { target: `${HOST}` },
  "/apps": { target: `${HOST}` },
  "^/theme.css": { target: `${HOST}/${SCHEMA}` },
};
