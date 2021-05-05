import { request, gql } from "graphql-request"

export default {
  fetchVariables: async ({ commit }) => {
    const query = gql`query Variables ($search: String, $filter: VariablesFilter) { 
      Variables (search: $search, filter: $filter){ 
        name,
        release {
          resource {
            acronym
          },
          version
        },
        label, 
        repeats { 
          name          
        },
        keywords {
          name
        }
      } 
    }`
    let variables = {
      "filter": {
        "release": 
          {"equals": [
            {"resource": {
               "acronym": "LifeCycle"
             },
             "version": "1.0.0"
            }]
          }
      }
    }

    const resp = await request('graphql', query, variables).catch(e => console.error(e))
    commit('setVariables', resp.Variables)
  },

  fetchVariableDetails: async ({ commit, state }, variableName) => {
    if(state.variableDetails[variableName]) {
      // cache hit
      return state.variableDetails[variableName]
    }
    // else fetch 
    const query = gql`query Variables ($filter: VariablesFilter) { 
      Variables (limit: 1, filter: $filter){ 
        name,
        label, 
        format {
          name
        },
        unit {
          name
        },
        description,
        repeats { 
          name
        } 
      } 
    }`
    const variables = {
      "filter": {
        "name": {
          "like": [`${variableName}`]
        },
        "release": {
          "equals": [
            {"resource": {
                "acronym": "LifeCycle"
              },
              "version": "1.0.0"
            }
          ]
        }
      }
    }

    const resp = await request('graphql', query, variables).catch(e => console.error(e))
    commit('setVariableDetails', { variableName, variableDetails: resp.Variables[0]})
  },

  fetchKeywords: async ({ commit }) => {
    const keywordQuery = gql`query Keywords  { 
      Keywords{ 
        name,
        definition,
        order
        parent {
          name
        }
      } 
    }`
    const keyWordResp = await request('graphql', keywordQuery).catch(e => console.error(e))
    commit('setKeywords', keyWordResp.Keywords)
  },

  fetchCohorts: async ({ commit }) => {
    const query = gql`query Databanks  { 
      Databanks{ 
        acronym,
        name,
        type {
          name 
        }
      } 
    }`
    //{filter: {type: {equals: [{name: "cohort"}, {name: "harmonisation"}]}}}
    const resp = await request('graphql', query).catch(e => console.error(e))
    commit('setCohorts', resp.Databanks)
  },

  fetchMappings: async ({ commit }) => {
    const query = gql`query VariableMappings ($filter: VariableMappingsFilter) { 
      VariableMappings (filter: $filter) { 
        fromTable {
          release {
            resource {
              acronym
            }
            version
          }
          name 
        }
        # toTable {
        #   release {
        #     resource {
        #       acronym
        #     }
        #   }
        # }
        toVariable {
          table {
            release {
              resource {
                acronym
              },
              version
            },
            name
          },
          name
        },
        match {
          name
        }
      } 
    }`
    //{filter: {type: {equals: [{name: "cohort"}, {name: "harmonisation"}]}}}
    const resp = await request('graphql', query).catch(e => console.error(e))
    commit('setVariableMappings', resp.VariableMappings)
  }
}