import { request } from "graphql-request";

let dataCache = {};
let queryCache = {};

const fetchById = async (query, type, variables) => {
  const dataKey = JSON.stringify(variables);
  if (!queryCache[query]) {
    queryCache[query] = await import(`../query/${query}.js`).catch(() => {
      console.error("failed to load query");
      return null;
    });
  }
  if (!dataCache[dataKey]) {
    dataCache[dataKey] = (
      await request("graphql", queryCache[query].default, variables).catch(
        (e) => {
          console.error(
            `Error fetching data for ${type} (id: ${JSON.stringify(variables)})`
          );
          console.error(e);
        }
      )
    )[type][0];
  }

  return dataCache[dataKey];
};

export { fetchById };
