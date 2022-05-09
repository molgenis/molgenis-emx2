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
  setSelectedCohorts(state, selectedCohorts) {
    state.filters.find((f) => f.name === "cohorts").conditions =
      selectedCohorts;
  },
  setSelectedKeywords(state, selectedKeywords) {
    state.filters.find((f) => f.name === "keywords").conditions =
      selectedKeywords;
  },
  setKeywords(state, keywords) {
    state.keywords = keywords;
  },
  setResources(state, resources) {
    Vue.set(state, "resources", resources);
  },
  setSchema(state, schema) {
    state.schema = schema;
  },
};
