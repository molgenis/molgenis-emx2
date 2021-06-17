import { request } from "graphql-request";
import cohorts from "../query/cohorts.gql";
let cache = null;

const fetchCohorts = async () => {
  if (!cache) {
    cache = (await request("graphql", cohorts).catch((e) => console.error(e)))
      .Cohorts;
  }

  return cache;
};

export { fetchCohorts };
