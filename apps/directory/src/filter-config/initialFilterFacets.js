export const initialFilterFacets = [
  // {
  //   component: 'MultiFilter',
  //   name: 'diagnosis_available',
  //   label: 'Diagnosis available',
  //   maxVisibleOptions: 100,
  //   tableName: 'eu_bbmri_eric_disease_types',
  //   columnName: 'diagnosis_available',
  //   humanReadableString: 'Disease type(s):',
  //   showFacet: true
  // },
  {
    facetTitle: 'Countries',
    component: 'CheckboxFilter',
    sourceTable: 'Countries',
    applyToColumn: 'country.name',
    filterValueAttribute: 'name',
    filterLabelAttribute: 'label',
    removeOptions: [],
    showMatchTypeSelector: false,
    negotiatorRequestString: 'Countries:',
    adaptive: false,
    sortColumn: 'label',
    sortDirection: 'asc',
    showFacet: true
  },
  {
    facetTitle: 'Collection type',
    component: 'CheckboxFilter',
    sourceTable: 'CollectionTypes',
    applyToColumn: 'collections.collectionType.name',
    filterValueAttribute: 'name',
    filterLabelAttribute: 'label',
    removeOptions: ['other'],
    showMatchTypeSelector: true,
    negotiatorRequestString: 'Collection type(s):',
    adaptive: false, // todo: set to true
    sortColumn: 'label',
    sortDirection: 'asc',
    showFacet: true
  },
  {
    facetTitle: 'Categories',
    component: 'CheckboxFilter',
    sourceTable: 'Categories',
    applyToColumn: 'collections.categories.name',
    filterValueAttribute: 'name',
    filterLabelAttribute: 'label',
    removeOptions: ['other'],
    showMatchTypeSelector: true,
    negotiatorRequestString: 'Categories:',
    adaptive: false, // todo: set to true
    sortColumn: 'label',
    sortDirection: 'asc',
    showFacet: true
  },

  // {
  //   name: 'materials',
  //   label: 'Material type',
  //   tableName: 'eu_bbmri_eric_material_types',
  //   columnName: 'materials',
  //   humanReadableString: 'Material type(s):',
  //   removeOptions: ['other'],
  //   showFacet: true,
  //   adaptive: true
  // },
  // {
  //   name: 'commercial_use',
  //   label: 'Collaboration type',
  //   columnName: 'collaboration_commercial',
  //   humanReadableString: 'Biobank collaboration type(s):',
  //   showFacet: true
  // },
  // {
  //   name: 'biobank_capabilities',
  //   label: 'Biobank services',
  //   tableName: 'eu_bbmri_eric_capabilities',
  //   columnName: 'capabilities',
  //   humanReadableString: 'Biobank services:',
  //   applyTo: ['eu_bbmri_eric_biobanks'],
  //   showFacet: false
  // },
  // {
  //   name: 'combined_quality',
  //   label: 'Quality labels',
  //   tableName: 'eu_bbmri_eric_assessment_levels',
  //   columnName: 'combined_quality',
  //   humanReadableString: 'Quality label(s):',
  //   showFacet: false
  // },
  // {
  //   name: 'network',
  //   label: 'Network',
  //   tableName: 'eu_bbmri_eric_networks',
  //   columnName: 'combined_network',
  //   humanReadableString: 'Network(s):',
  //   showFacet: false
  // },
  // {
  //   name: 'dataType',
  //   label: 'Data type',
  //   tableName: 'eu_bbmri_eric_data_types',
  //   columnName: 'data_categories',
  //   humanReadableString: 'Data type(s):',
  //   removeOptions: ['other'],
  //   showFacet: false
  // },
  {
    facetTitle: 'search',
    component: "StringFilter",
    humanReadableString: 'Text search is',
    builtIn: true /** search should not be generated */
  }
]
