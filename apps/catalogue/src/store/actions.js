import { request, gql } from "graphql-request";
import schema from "./query/schema.gql";
import mappings from "./query/mappings.gql";
import { fetchResources } from "./repository/resourceRepository";

export default {
  fetchSchema: async ({ state, commit }) => {
    const resp = await request("graphql", schema).catch((e) => {
      console.error(e);
      state.isLoading = false;
    });
    commit("setSchema", resp._schema.name);
    return resp._schema.name;
  },
  fetchVariables: async ({ state, commit, getters }, offset = 0) => {
    state.isLoading = true;
    const query = gql`
      query Variables($search: String, $filter: VariablesFilter) {
        Variables(limit: 100, offset: ${offset}, search: $search, filter: $filter) {
          name
          release {
            resource {
              acronym
            }
            version
          }
          label
          repeats {
            name
          }
        }
        Variables_agg(search: $search, filter: $filter) {
          count
        }
      }
    `;

    let queryVariables = { filter: {} };

    queryVariables.filter = {
      release: {
        resource: {
          mg_tableclass: { equals: [`${state.schema}.Networks`] },
        },
      },
    };

    if (getters.selectedNetworks.length) {
      queryVariables.filter.release = {
        equals: getters.selectedNetworks.map((selectedNetwork) => {
          return {
            version: "1.0.0",
            resource: selectedNetwork,
          };
        }),
      };
    }

    if (getters.selectedKeywords.length) {
      queryVariables.filter.keywords = {
        equals: getters.selectedKeywords.map((sk) => ({ name: sk })),
      };
    }

    if (getters.searchString) {
      queryVariables.search = getters.searchString;
    }

    const resp = await request("graphql", query, queryVariables).catch((e) => {
      console.error(e);
      state.isLoading = false;
    });

    // check if result is still the relevant
    if (
      getters.searchString === null ||
      getters.searchString === queryVariables.search
    ) {
      if (offset === 0) {
        commit("setVariables", resp.Variables);
      } else {
        // add for show more variables
        commit("addVariables", resp.Variables);
      }
      commit("setVariableCount", resp.Variables_agg.count);
      state.isLoading = false;
    }

    return resp.Variables;
  },

  fetchAdditionalVariables: async ({ state, dispatch }) => {
    dispatch("fetchVariables", state.variables.length);
  },

  fetchVariableDetails: async (context, variable) => {
    const query = gql`
      query Variables($filter: VariablesFilter) {
        Variables(limit: 1, filter: $filter) {
          name
          label
          format {
            name
          }
          unit {
            name
          }
          description
          permittedValues {
            value
            label
            order
            isMissing
            ontologyTermIRI
          }
          repeats {
            name
          }
        }
      }
    `;
    const variables = {
      filter: {
        name: {
          equals: [`${variable.name}`],
        },
        release: {
          equals: [
            {
              resource: {
                acronym: variable.release.resource.acronym,
              },
              version: variable.release.version,
            },
          ],
        },
      },
    };

    const resp = await request("graphql", query, variables).catch((e) =>
      console.error(e)
    );

    let variableDetails = resp.Variables[0];

    const mappingQuery = gql`
      query VariableMappings($filter: VariableMappingsFilter) {
        VariableMappings(filter: $filter) {
          fromTable {
            release {
              resource {
                acronym
              }
              version
            }
            name
          }
          match {
            name
          }
        }
      }
    `;

    const mappingQueryVariables = {
      filter: {
        toVariable: {
          equals: [
            {
              release: {
                resource: {
                  acronym: variable.release.resource.acronym,
                },
                version: variable.release.version,
              },
              name: variable.name,
            },
          ],
        },
      },
    };

    const mappingQueryResp = await request(
      "graphql",
      mappingQuery,
      mappingQueryVariables
    ).catch((e) => console.error(e));

    // Put list in to map, use acronym as key
    if (
      mappingQueryResp.VariableMappings &&
      mappingQueryResp.VariableMappings.length
    ) {
      const mappingsByAcronym = mappingQueryResp.VariableMappings.reduce(
        (accum, item) => {
          accum[item.fromTable.release.resource.acronym] = item;
          return accum;
        },
        {}
      );
      variableDetails.mappings = mappingsByAcronym;
    }

    return variableDetails;
  },

  fetchKeywords: async ({ state, commit }) => {
    if (state.keywords.length) {
      return state.keywords;
    }

    const keywordQuery = gql`
      query Keywords {
        Keywords {
          name
          definition
          order
          parent {
            name
          }
        }
      }
    `;
    const keyWordResp = await request("graphql", keywordQuery).catch((e) =>
      console.error(e)
    );
    commit("setKeywords", keyWordResp.Keywords);
    return state.keywords;
  },

  fetchResources: async ({ commit }) => {
    const resources = await fetchResources();
    commit("setResources", resources);
  },

  fetchMappings: async (context, variable) => {
    let nameFilter = [variable.name];
    if (variable.repeats) {
      nameFilter = nameFilter.concat(variable.repeats.map((r) => r.name));
    }

    const filter = {
      toVariable: {
        name: {
          equals: nameFilter,
        },
        release: {
          resource: {
            equals: {
              acronym: variable.release.resource.acronym,
            },
          },
          version: {
            equals: variable.release.version,
          },
        },
      },
    };

    const resp = await request("graphql", mappings, { filter }).catch((e) =>
      console.error(e)
    );
    return resp.VariableMappings;
  },
};
