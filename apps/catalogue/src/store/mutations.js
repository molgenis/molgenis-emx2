import Vue from 'vue'

export default {
  setVariables (state, variables) {
    Vue.set(state, 'variables', variables)
  },
  setVariableCount (state, variableCount) {
    state.variableCount = variableCount
  },
  setVariableDetails (state, {variableName, variableDetails}) {
    Vue.set(state.variableDetails, variableName, variableDetails)
  },
  setFilters(state, filters) {
    state.filters = filters
  },
  setVariableSearch(state, variableSearch) {
    state.variableSearch = variableSearch
  },
  setKeywords(state, keywords) {
    state.keywords = keywords.map(item => ({
      id: item.name,
      label: item.definition,
      parent: item.parent ? item.parent.name : null,
      order: item.order
    }))
  },
  setDatabanks (state, databanks) {    
    state.databanks = databanks
  },
  setNetworks (state, networks) {
    state.networks = networks
  },
  setVariableMappings (state, variableMappings) {
    Vue.set(state, 'variableMappings', variableMappings)
  },
  removeKeywordFromSelection(state, keywordName) {
    state.selectedKeywords = state.selectedKeywords.filter(sk => sk !== keywordName)
  }
}
  