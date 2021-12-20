const HOST = process.env.MOLGENIS_APPS_HOST || "https://brenda2.molgeniscloud.org/";
const SCHEMA = process.env.MOLGENIS_APPS_SCHEMA || "catalogue-pelagie-issue-3";

module.exports = {
  "^/graphql": {
    target: `${HOST}/${SCHEMA}`,
  },
  "/api": { target: `${HOST}` },
  "/apps": { target: `${HOST}` },
  "^/theme.css": { target: `${HOST}/${SCHEMA}` },
};
