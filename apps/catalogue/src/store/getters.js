import state from "./state"

export default {
  variables: (state) => state.variables,
  variableCount: (state) => state.variableCount,
  variableDetails: (state) => state.variableDetails,
  keywords: (state) => state.keywords,
  selectedKeywords: (state) => state.selectedKeywords,
  harmonizations: (state) => state.harmonizations
}
