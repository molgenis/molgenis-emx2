export default {
  variables: (state) => state.variables,
  variableCount: (state) => state.variableCount,
  variableDetails: (state) => state.variableDetails,
  variableDetailsByName: (state) => (name) => state.variableDetails[name],
  mappingDetailsByVariableAndMapping: (state) => (name, mapping) => {
    if (
      state.variableDetails[name] &&
      state.variableDetails[name].mappings[mapping] &&
      state.variableDetails[name].mappings[mapping].details
    ) {
      return state.variableDetails[name].mappings[mapping].details;
    } else {
      return {};
    }
  },
  searchString: (state) =>
    state.searchInput === null || state.searchInput.trim() === ""
      ? null
      : state.searchInput.trim(),
  selectedKeywords: (state) => {
    return state.filters.find((filters) => filters.name === "keywords")
      .conditions;
  },
  selectedNetworks: (state) => {
    return state.filters.find((filters) => filters.name === "networks")
      .conditions;
  },
  selectedCohorts: (state) => {
    return state.filters.find((filters) => filters.name === "cohorts")
      .conditions;
  },
  resources: (state) => state.resources,
  /**
   * @returns Grid like object o[x][y], where;
   *  x = variableName,
   *  y = cohortPid
   *  and cell value is match status
   *
   * @example
   * {
   *   gender: {
   *     alspac: 'matched'
   *   }
   * }
   *
   */
  harmonizationGrid: (state) => {
    const harmonizationGrid = {};

    state.variableMappings.forEach((mapping) => {
      const varName = mapping.toVariable.name;
      if (!harmonizationGrid[varName]) {
        harmonizationGrid[varName] = {};
      }
      const mappedCohort = mapping.fromTable.dataDictionary.resource.pid;
      harmonizationGrid[varName][mappedCohort] = mapping.match.name; // aka the cell value
    });

    return harmonizationGrid;
  },
};
