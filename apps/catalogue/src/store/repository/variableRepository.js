import { request } from "graphql-request";
import variableDetails from "../query/variableDetails.gql";

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

export { fetchDetails };
