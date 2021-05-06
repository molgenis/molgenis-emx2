export default {
  getSearchFilters: (state) => (variables) => {
    if(state.variableSearch.length > 0) {
      variables.search = state.variableSearch
    }
    return variables
  },
  getKeywordFilters: (state) => (variables) => {
    if(state.filters.keywords && state.filters.keywords.length) {
      variables.filter.keywords = {
        "equals": state.filters.keywords.map(keyword => ({ name: keyword }))
      }
    }
    return variables
  },
  getAcronymFilters: (state) => (variables) => {    
    const databanksFilter = state.filters.databanks && state.filters.databanks.map(databank => ({ resource: { acronym: databank }, version: "1.0.0" })) || []
    const networksFilter = state.filters.networks && state.filters.networks.map(network => ({ resource: { acronym: network }, version: "1.0.0" })) || []

    variables.filter.release = { "equals": [...databanksFilter, ...networksFilter] }

    return variables
  },
  getMapping: (state) => (variable, cohort) => {
    const mapping = state.variableMappings.find(mapping => mapping.toVariable.name === variable && mapping.fromTable.release.resource.acronym === cohort)
    const toVariable = state.variables.find(variable => variable.name === mapping.toVariable.name)
    return {
        variable: {
          name: toVariable.name, 
          description: toVariable.label
        }, 
        sources: mapping.fromVariable, 
        syntax: mapping.syntax
      }
  },

  /**
   * @returns Grid like object o[x][y], where;
   *  x = variableName,
   *  y = cohortAcronym 
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
  mappings: (state) => {
    const harmonizationGrid = {}

    state.variableMappings.forEach(mapping => {
      const varName = mapping.toVariable.name
      if(!harmonizationGrid[varName]) {
        harmonizationGrid[varName] = {}
      }
      const mappedCohort = mapping.fromTable.release.resource.acronym
      harmonizationGrid[varName][mappedCohort] = mapping.match.name // aka the cell value
    })

    return harmonizationGrid    
  }
}
