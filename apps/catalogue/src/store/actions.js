import { request, gql } from "graphql-request";
import schema from "./query/schema.js";
import mappings from "./query/mappings.js";
import { fetchResources } from "./repository/resourceRepository";

export default {
  reloadMetadata({ state }) {
    state.isLoading = true;
    state.graphqlError = null;
    request(
      state.graphqlURL,
      `{
          _session { email,roles } _schema {
            id, label, tables {
              id, label, tableType, description, schemaId, semantics, columns {
                id, label, columnType, key, refTableId, refLinkId, refLabel, refBackId, required, 
                semantics, description, position, validation, visible
              } settings { key, value }
            }
          }
        }`
    )
      .then((data) => {
        state.session = data._session;
        state.schema = data._schema;
      })
      .catch((error) => {
        if (Array.isArray(error.response.errors)) {
          state.graphqlError = error.response.errors[0].message;
        } else {
          state.graphqlError = error;
        }
      });
    state.isLoading = false;
  },
  fetchSchema: async ({ state, commit }) => {
    const resp = await request("graphql", schema).catch((e) => {
      console.error(e);
      state.isLoading = false;
    });
    commit("setSchema", resp._schema.id);
    return resp._schema.id;
  },
  fetchVariables: async ({ state, commit, getters, dispatch }, offset = 0) => {
    state.isLoading = true;
    commit("setVariableCount", "...loading count");
    const query = gql`
            query Variables($search: String, $filter: VariablesFilter) {
                Variables(limit: 50, offset: ${offset}, search: $search, filter: $filter, orderby:{label: ASC}) {
                  name
                  keywords {
                    name
                  }
                  description
                  unit {
                    name
                  }
                  format {
                    name
                  }
                  dataset {
                    name
                  }
                  resource {
                    id
                    name
                  }
                  label
                  repeats {
                    name
                  }
                  mappings {
                    source {
                      id
                    }
                    targetVariable {
                      name
                    }
                    targetDataset {
                      resource {
                        id
                      }
                    }
                    sourceDataset {
                      resource {
                        id
                      }
                    }
                    match {
                      name
                    }
                    syntax
                    description
                    sourceVariablesOtherDatasets {
                      dataset {
                        name
                      }
                      name
                    }
                  }
                  permittedValues {
                    value
                    label
                  }
                }
                Variables_agg(search: $search, filter: $filter) {
                    count
                }
            }
        `;

    let queryVariables = { filter: {} };

    queryVariables.filter = {
      resource: { mg_tableclass: { like: ["Models"] } },
    }; //all target variables are valid

    if (getters.selectedNetworks.length) {
      const networkModels = await dispatch(
        "fetchNetworkModels",
        getters.selectedNetworks
      );

      queryVariables.filter.resource = {
        equals: networkModels.map((model) => {
          return {
            id: model.id,
          };
        }),
      };
    }

    if (getters.selectedCohorts.length) {
      queryVariables.filter.mappings = {
        source: {
          id: {
            equals: getters.selectedCohorts.map((cohort) => cohort.id),
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
    }
    state.isLoading = false;

    return resp.TargetVariables;
  },

  fetchAdditionalVariables: async ({ state, dispatch }) => {
    dispatch("fetchVariables", state.variables.length);
  },

  fetchVariableDetails: async (context, variable) => {
    const query = gql`
      query TargetVariables($filter: VariablesFilter) {
        Variables(limit: 1, filter: $filter, orderby: { name: ASC }) {
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
            ontologyTermURI
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
        dataset: {
          equals: [
            {
              resource: {
                id: variable.resource.id,
              },
              name: variable.dataset.name,
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
        VariableMappings(filter: $filter, orderby: { source: ASC }) {
          source {
            id
          }
          sourceDataset {
            resource {
              id
            }
          }
          match {
            name
          }
        }
      }
    `;

    const mappingQueryVariables = {
      filter: {
        targetVariable: {
          equals: [
            {
              resource: {
                id: variable.resource.id,
              },
              dataset: {
                name: variable.dataset.name,
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

    // Put list in to map, use id as key
    if (
      mappingQueryResp.VariableMappings &&
      mappingQueryResp.VariableMappings.length
    ) {
      const mappingsById = mappingQueryResp.VariableMappings.reduce(
        (accum, item) => {
          accum[item.source.id] = item;
          return accum;
        },
        {}
      );
      variableDetails.mappings = mappingsById;
    }

    return variableDetails;
  },

  fetchCohorts: async ({ state, commit }) => {
    if (state.cohorts.length) {
      return state.cohorts;
    }

    const cohortQuery = gql`
      query Cohorts {
        Cohorts(orderby: { id: ASC }) {
          id
          networks {
            id
          }
        }
      }
    `;

    const cohortResp = await request("graphql", cohortQuery).catch((e) =>
      console.error(e)
    );
    commit("setCohorts", cohortResp.Cohorts);
    return state.cohorts;
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
    const keyWordResp = await request(
      "/CatalogueOntologies/graphql",
      keywordQuery
    ).catch((e) => console.error(e));
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
      targetVariable: {
        name: {
          equals: nameFilter,
        },
        resource: {
          equals: {
            id: variable.resource.id,
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
          id
          models {
            id
          }
        }
      }
    `;

    const filter = {
      id: { equals: selectedNetworks.map((sn) => sn.id) },
    };

    const resp = await request("graphql", query, { filter }).catch((e) =>
      console.error(e)
    );

    return resp.Networks.flatMap((n) => n.models).filter((m) => m != undefined);
  },
};
