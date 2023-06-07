import { request } from "graphql-request";
import resources from "../query/resources.js";
let cache = null;

const fetchResources = async () => {
  if (!cache) {
    cache = (
      await request("graphql", resources).catch(e =>
        console.error("resources not found: " + e)
      )
    ).Resources;
  }

  return cache;
};

export { fetchResources };
