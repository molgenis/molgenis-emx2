import Vue from "vue";

export default {
  setVariables(state, variables) {
    Vue.set(state, "variables", Array.isArray(variables) ? variables : []);
  },
  addVariables(state, variables) {
    state.variables = state.variables.concat(variables);
  },
  setVariableCount(state, variableCount) {
    state.variableCount = variableCount;
  },
  setVariableDetails(state, { variableName, variableDetails }) {
    Vue.set(state.variableDetails, variableName, variableDetails);
  },
  setVariableMappingDetails(state, { variableName, mappingName, details }) {
    Vue.set(
      state.variableDetails[variableName].mappings[mappingName],
      "details",
      details
    );
  },
  setSearchInput(state, searchInput) {
    state.searchInput = searchInput;
  },
  setSelectedNetworks(state, selectedNetworks) {
    state.filters.find((f) => f.name === "networks").conditions =
      selectedNetworks;
  },
  setKeywords(state, keywords) {
    state.keywords = keywords;
  },
  setCohorts(state, cohorts) {
    Vue.set(state, "cohorts", cohorts);
  },
  setVariableMappings(state, variableMappings) {
    Vue.set(state, "variableMappings", variableMappings);
  },
  setSchema(state, schema) {
    state.schema = schema
  }
};
