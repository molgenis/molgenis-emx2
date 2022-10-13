import { request } from "graphql-request";
import variableDetails from "../query/variableDetails.js";
import fromVariableDetails from "../query/fromVariableDetails.js";

const fetchDetails = async (name, model, version) => {
  const params = {
    filter: {
      name: { equals: name },
      dataDictionary: {
        equals: [
          {
            resource: {
              pid: model,
            },
            version: version,
          },
        ],
      },
    },
  };

  return (
    await request("graphql", variableDetails, params).catch((e) =>
      console.error("fetch variableDetails details failed: " + e)
    )
  ).TargetVariables[0];
};

const fetchFromVariableDetails = async (names, network, version) => {
  const params = {
    filter: {
      name: { equals: names },
      dataDictionary: {
        equals: [{ resource: { pid: network }, version: version }],
      },
    },
  };

  return (
    await request("graphql", fromVariableDetails, params).catch((e) =>
      console.error("fetch fromVariableDetails details failed: " + e)
    )
  ).SourceVariables[0];
};

export { fetchDetails, fetchFromVariableDetails };
