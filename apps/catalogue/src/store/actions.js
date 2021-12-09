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
      query Variables($search: String, $filter: VariablesFilter) {
        Variables(limit: 100, offset: ${offset}, search: $search, filter: $filter) {
          name
          release {
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
        Variables_agg(search: $search, filter: $filter) {
          count
        }
      }
    `;

    let queryVariables = { filter: {} };

    queryVariables.filter = {
      release: {
        resource: {
          mg_tableclass: { equals: [`${state.schema}.Models`] },
        },
      },
    };

    if (getters.selectedNetworks.length) {
      const networkModels = await dispatch(
        "fetchNetworkModels",
        getters.selectedNetworks
      );

      queryVariables.filter.release = {
        equals: networkModels.map((model) => {
          return {
            // version: "1.0.0",
            resource: model,
          };
        }),
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
              pid: variable.release.resource.pid,
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
