import { request } from "graphql-request";
import variableDetails from "../query/variableDetails.gql";
import fromVariableDetails from "../query/fromVariableDetails.gql";

const fetchDetails = async (name, network, version) => {
  const params = {
    filter: {
      name: { equals: name },
      release: {
        equals: [
          {
            resource: {
              acronym: network,
            },
            version: version,
          },
        ],
      },
    },
  };

  return (
    await request("graphql", variableDetails, params).catch((e) =>
      console.error(e)
    )
  ).Variables[0];
};

const fetchFromVariableDetails = async (names, schema, version) => {
  const params = {
    filter: {
      name: { equals: names },
      release: {
        version: { equals: version },
        resource: {
          mg_tableclass: { equals: [`${schema}.Databanks`] },
        },
      },
    },
  };

  return (
    await request("graphql", fromVariableDetails, params).catch((e) =>
      console.error(e)
    )
  ).Variables;
};

export { fetchDetails, fetchFromVariableDetails };
