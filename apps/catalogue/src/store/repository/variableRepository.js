import { request } from "graphql-request";
import variableDetails from "../query/variableDetails.js";
import fromVariableDetails from "../query/fromVariableDetails.js";

const fetchDetails = async (name, model) => {
  const params = {
    filter: {
      name: { equals: name },
      resource: {
        equals: [
          {
            id: model,
          },
        ],
      },
    },
  };

  return (
    await request("graphql", variableDetails, params).catch((e) =>
      console.error("fetch variableDetails details failed: " + e)
    )
  ).Variables[0];
};

const fetchFromVariableDetails = async (names, network) => {
  const params = {
    filter: {
      name: { equals: names },
      resource: {
        equals: { id: network },
      },
    },
  };

  return (
    await request("graphql", fromVariableDetails, params).catch((e) =>
      console.error("fetch fromVariableDetails details failed: " + e)
    )
  ).Variables[0];
};

export { fetchDetails, fetchFromVariableDetails };
