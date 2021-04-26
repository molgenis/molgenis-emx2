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
  setKeywords (state, keywords) {
    state.keywords = keywords
  },
}
  