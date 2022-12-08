import { request } from "graphql-request";
import cohortQuery from "../query/cohortDetails.js";
let cache = {};

const fetchById = async (pid) => {
  if (!cache[pid]) {
    cache[pid] = (
      await request("graphql", cohortQuery, { pid }).catch((e) => {
        console.error(`Error fetching data for cohort (pid: ${pid})`);
        console.error(e);
      })
    ).Cohorts[0];
  }

  return cache[pid];
};

export { fetchById };
