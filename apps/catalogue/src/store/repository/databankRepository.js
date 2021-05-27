import { request } from "graphql-request";
import databanks from "../query/databanks.gql";
let cache = null;

const fetchDatabanks = async () => {
  if (!cache) {
    cache = (await request("graphql", databanks).catch((e) => console.error(e)))
      .Databanks;
  }

  return cache;
};

export { fetchDatabanks };
