import state from "./state"

export default {
  filteredVariables: (state) => {
    if(state.filters.keywords && state.filters.keywords.length > 0) {
      return state.variables.filter((variable) => {
        return variable.keywords && state.filters.keywords.some(keyword => variable.keywords.map(keyword => keyword.name).includes(keyword))
      })
    } else {
      return state.variables
    }
  },
  variableCount: (state, {filteredVariables}) => filteredVariables.length,

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
  harmonizationGrid: (state) => {
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
