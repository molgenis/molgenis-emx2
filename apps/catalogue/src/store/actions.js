import { request, gql } from "graphql-request"

export default {
  fetchVariables: async ({ commit, getters }) => {
    const query = gql`query Variables ($search: String, $filter: VariablesFilter) { 
      Variables (limit: 100, search: $search, filter: $filter){ 
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
      Variables_agg(filter: $filter, search: $search){
        count
      }
    }`
    
    let variables = {
      "filter": {}
    }

    variables = getters.getSearchFilters(variables)
    variables = getters.getKeywordFilters(variables)
    variables = getters.getAcronymFilters(variables)

    const resp = await request('graphql', query, variables).catch(e => console.error(e))
    commit('setVariables', resp.Variables)
    commit('setVariableCount', resp.Variables_agg.count)
  },

  fetchVariableDetails: async ({ commit, state, getters }, variableName) => {
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
    let variables = {
      "filter": {
        "name": {
          "like": [`${variableName}`]
        }
      }
    }

    variables = getters.getAcronymFilters(variables)

    const resp = await request('graphql', query, variables).catch(e => console.error(e))
    commit('setVariableDetails', { variableName, variableDetails: resp.Variables[0]})
  },

  fetchKeywords: async ({ commit }) => {
    const query = gql`query Keywords  { 
      Keywords{ 
        name,
        definition,
        order
        parent {
          name
        }
      } 
    }`
    const resp = await request('graphql', query).catch(e => console.error(e))
    commit('setKeywords', resp.Keywords)
  },

  fetchDatabanks: async ({ commit }) => {
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
    commit('setDatabanks', resp.Databanks)
  },

  fetchNetworks: async ({ commit }) => {
    const query = gql`query Networks  { 
      Networks{ 
        acronym,
        name
      } 
    }`
    //{filter: {type: {equals: [{name: "cohort"}, {name: "harmonisation"}]}}}
    const resp = await request('graphql', query).catch(e => console.error(e))
    commit('setNetworks', resp.Networks)
  },

  fetchMappings: async ({ commit, state, getters }) => {
    const query = gql`query VariableMappings ($filter: VariableMappingsFilter) { 
      VariableMappings (limit: 100, filter: $filter) { 
        fromTable {
          release {
            resource {
              acronym
            }
            version
          }
          name 
        }
        fromVariable {
          name
        }
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
        syntax
      } 
    }`

    let variables = state.variables.map(variable => ({ name: variable.name }))
    const filter = variables.length ? {
      'filter': { 'toVariable':  { 'equals': variables } }
    } : {}

    variables = getters.getAcronymFilters(variables)

    const resp = await request('graphql', query, filter).catch(e => console.error(e))
    commit('setVariableMappings', resp.VariableMappings)
  },
}