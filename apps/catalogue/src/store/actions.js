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
  fetchVariables: async ({ state, commit, getters, dispatch }, offset = 0) => {
    state.isLoading = true;
    const query = gql`
            query TargetVariables($search: String, $filter: TargetVariablesFilter) {
                TargetVariables(limit: 100, offset: ${offset}, search: $search, filter: $filter, orderby:{label: ASC}) {
                    name
                    dataDictionary {
                        resource {
                            pid
                        }
                        version
                    }
                    label
                    repeats {
                        name
                    }
                }
                TargetVariables_agg(search: $search, filter: $filter) {
                    count
                }
            }
        `;

    let queryVariables = { filter: {} };

    queryVariables.filter = {}; //all target variables are valid

    if (getters.selectedNetworks.length) {
      const networkModels = await dispatch(
        "fetchNetworkModels",
        getters.selectedNetworks
      );

      queryVariables.filter.dataDictionary = {
        equals: networkModels.map((model) => {
          return {
            // version: "1.0.0",
            resource: model,
          };
        }),
      };
    }

    if (getters.selectedCohorts.length) {
      queryVariables.filter.mappings = {
        fromDataDictionary: {
          resource: {
            pid: {
              equals: getters.selectedCohorts.map((cohort) => cohort.pid),
            },
          },
        },
      };
    }
    if (getters.selectedKeywords.length) {
      queryVariables.filter.keywords = {
        equals: getters.selectedKeywords,
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
        commit("setVariables", resp.TargetVariables);
      } else {
        // add for show more variables
        commit("addVariables", resp.TargetVariables);
      }
      commit("setVariableCount", resp.TargetVariables_agg.count);
      state.isLoading = false;
    }

    return resp.TargetVariables;
  },

  fetchAdditionalVariables: async ({ state, dispatch }) => {
    dispatch("fetchVariables", state.variables.length);
  },

  fetchVariableDetails: async (context, variable) => {
    const query = gql`
      query TargetVariables($filter: TargetVariablesFilter) {
        TargetVariables(limit: 1, filter: $filter) {
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
        dataDictionary: {
          equals: [
            {
              resource: {
                pid: variable.dataDictionary.resource.pid,
              },
              version: variable.dataDictionary.version,
            },
          ],
        },
      },
    };

    const resp = await request("graphql", query, variables).catch((e) =>
      console.error(e)
    );

    let variableDetails = resp.TargetVariables[0];

    const mappingQuery = gql`
      query VariableMappings($filter: VariableMappingsFilter) {
        VariableMappings(
          filter: $filter
          orderby: { fromDataDictionary: ASC }
        ) {
          fromTable {
            dataDictionary {
              resource {
                pid
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
              dataDictionary: {
                resource: {
                  pid: variable.dataDictionary.resource.pid,
                },
                version: variable.dataDictionary.version,
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

    // Put list in to map, use pid as key
    if (
      mappingQueryResp.VariableMappings &&
      mappingQueryResp.VariableMappings.length
    ) {
      const mappingsByPid = mappingQueryResp.VariableMappings.reduce(
        (accum, item) => {
          accum[item.fromTable.dataDictionary.resource.pid] = item;
          return accum;
        },
        {}
      );
      variableDetails.mappings = mappingsByPid;
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
        dataDictionary: {
          resource: {
            equals: {
              pid: variable.dataDictionary.resource.pid,
            },
          },
          version: {
            equals: variable.dataDictionary.version,
          },
        },
      },
    };

    const resp = await request("graphql", mappings, { filter }).catch((e) =>
      console.error(e)
    );
    return resp.VariableMappings;
  },

  fetchNetworkModels: async (context, selectedNetworks) => {
    const query = gql`
      query Networks($filter: NetworksFilter) {
        Networks(filter: $filter) {
          pid
          models {
            pid
          }
        }
      }
    `;

    const filter = {
      pid: { equals: selectedNetworks.map((sn) => sn.pid) },
    };

    const resp = await request("graphql", query, { filter }).catch((e) =>
      console.error(e)
    );

    return resp.Networks.flatMap((n) => n.models).filter((m) => m != undefined);
  },
};
