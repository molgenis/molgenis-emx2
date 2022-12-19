import { genericFilterOptions, diagnosisAvailableFilterOptions, collaborationTypeFilterOptions } from '../utils/filterOptions'

export const filterTemplate = {

  component: 'CheckboxFilter',
  name: '',
  label: 'New filter',
  tableName: '',
  columnName: '',
  humanReadableString: '',
  showFacet: true,
  filterLabelAttribute: '',
  initialDisplayItems: 100,
  maxVisibleOptions: 25,
  headerClass: '',
  showSatisfyAllSelector: true,
  queryOptions: '',
  removeOptions: [],
  applyTo: []
}

export const createFilters = (state) => {
  const filterFacets = []

  for (const facet of state.filterFacets) {
    filterFacets.push(
      {
        headerClass: facet.headerClass || '',
        component: facet.component || 'CheckboxFilter',
        name: facet.name || facet.columnName, // name is needed for displaying the bookmark as of now. EG commercial_use is a boolean.
        label: facet.facetTitle || facet.label || facet.columnName,
        tableName: facet.tableName,
        columnName: facet.columnName,
        filterLabelAttribute: facet.filterLabelAttribute || '',
        options: getFilterOptions(facet), // uses the removeOptions array
        filters: state.filters.selections[facet.name], // adds the currently active options
        satisfyAll: state.filters.satisfyAll.includes(facet.name),
        initialDisplayItems: facet.initialDisplayItems || 100,
        maxVisibleOptions: facet.maxVisibleOptions || 50,
        showSatisfyAllSelector: facet.showSatisfyAllSelector || true,
        humanReadableString: facet.humanReadableString,
        builtIn: facet.builtIn,
        applyTo: facet.applyTo || ['eu_bbmri_eric_collections'],
        showFacet: facet.showFacet,
        adaptive: facet.adaptive
      })
  }

  return filterFacets
}

function getFilterOptions (filterFacet) {
  let options

  switch (filterFacet.name) {
    case 'diagnosis_available':
      options = diagnosisAvailableFilterOptions(filterFacet.tableName, filterFacet.columnName)
      break
    case 'commercial_use':
      options = collaborationTypeFilterOptions()
      break
    default:
      options = genericFilterOptions(filterFacet)
  }
  return options
}
