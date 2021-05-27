import { request, gql } from "graphql-request";

export default {
  fetchVariables: async ({ state, commit, getters }) => {
    state.isLoading = true;
    const query = gql`
      query Variables($search: String, $filter: VariablesFilter) {
        Variables(limit: 100, search: $search, filter: $filter) {
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
    let queryVariables = {
      filter: {
        release: {
          equals: [
            {
              resource: {
                acronym: "LifeCycle",
              },
              version: "1.0.0",
            },
          ],
        },
      },
    };

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
      commit("setVariables", resp.Variables);
      commit("setVariableCount", resp.Variables_agg.count);
      state.isLoading = false;
    }
  },

  fetchVariableDetails: async ({ commit, getters }, variableName) => {
    if (getters.variableDetails[variableName]) {
      // cache hit
      return getters.variableDetails[variableName];
    }
    // else fetch
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
          repeats {
            name
          }
        }
      }
    `;
    const variables = {
      filter: {
        name: {
          equals: [`${variableName}`],
        },
        release: {
          equals: [
            {
              resource: {
                acronym: "LifeCycle",
              },
              version: "1.0.0",
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
                  acronym: "LifeCycle",
                },
                version: "1.0.0",
              },
              name: variableName,
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
    const mappingsByAcronym = mappingQueryResp.VariableMappings.reduce(
      (accum, item) => {
        accum[item.fromTable.release.resource.acronym] = item;
        return accum;
      },
      {}
    );
    variableDetails.mappings = mappingsByAcronym;

    commit("setVariableDetails", {
      variableName,
      variableDetails,
    });

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

  fetchCohorts: async ({ commit }) => {
    const query = gql`
      query Databanks {
        Databanks {
          acronym
          name
          type {
            name
          }
        }
      }
    `;
    //{filter: {type: {equals: [{name: "cohort"}, {name: "harmonisation"}]}}}
    const resp = await request("graphql", query).catch((e) => console.error(e));
    commit("setCohorts", resp.Databanks);
  },

  fetchMappings: async ({ commit, getters }) => {
    const query = gql`
      query VariableMappings($filter: VariableMappingsFilter) {
        VariableMappings(limit: 100, filter: $filter) {
          fromTable {
            release {
              resource {
                acronym
              }
              version
            }
            name
          }
          toVariable {
            table {
              release {
                resource {
                  acronym
                }
                version
              }
              name
            }
            name
          }
          match {
            name
          }
        }
      }
    `;

    const variables = getters.variables.map((v) => {
      return {
        release: {
          resource: {
            acronym: "LifeCycle",
          },
          version: "1.0.0",
        },
        name: v.name,
      };
    });

    const filter = variables.length
      ? {
          filter: { toVariable: { equals: variables } },
        }
      : {};

    const resp = await request("graphql", query, filter).catch((e) =>
      console.error(e)
    );
    commit("setVariableMappings", resp.VariableMappings);
  },

  fetchMappingDetails: async ({ commit, getters }, { name, acronym }) => {
    if (!getters.variableDetails[name]) {
      return undefined;
    }
    const query = gql`
      query VariableMappings($filter: VariableMappingsFilter) {
        VariableMappings(filter: $filter) {
          syntax
          description
          match {
            name
          }
          fromVariable {
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
                  acronym: "LifeCycle",
                },
                version: "1.0.0",
              },
              name: name,
            },
          ],
        },
        fromTable: {
          equals: [
            {
              release: {
                resource: {
                  acronym: acronym,
                },
              },
            },
          ],
        },
      },
    };

    const resp = await request("graphql", query, mappingQueryVariables).catch(
      (e) => console.error(e)
    );

    commit("setVariableMappingDetails", {
      variableName: name,
      mappingName: acronym,
      details: resp.VariableMappings[0],
    });
  },
};
