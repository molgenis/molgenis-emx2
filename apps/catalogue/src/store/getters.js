export default {
  getMapping: (state) => (variable, cohort) => {
    const mapping = state.variableMappings.find(mapping => mapping.toVariable.name === variable && mapping.fromTable.release.resource.acronym === cohort)
    const toVariable = state.variables.find(variable => variable.name === mapping.toVariable.name)
    console.log(toVariable)
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
