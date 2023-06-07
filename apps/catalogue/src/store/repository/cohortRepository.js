import { request } from "graphql-request";
import cohortQuery from "../query/cohortDetails.js";

let cache = {};

const fetchById = async id => {
  if (!cache[id]) {
    cache[id] = (
      await request("graphql", cohortQuery, { id }).catch(e => {
        console.error(`Error fetching data for cohort (id: ${id})`);
        console.error(e);
      })
    ).Cohorts[0];
  }

  return cache[id];
};

export { fetchById };
