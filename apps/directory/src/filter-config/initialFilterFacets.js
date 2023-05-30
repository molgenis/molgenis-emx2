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
    facetTitle: 'Diagnosis available',
    component: 'OntologyFilter',
    sourceTable: 'DiseaseTypes',
    applyToColumn: 'collections.diseaseTypes.name',
    filterValueAttribute: 'name',
    filterLabelAttribute: 'label',
    removeOptions: [],
    ontologyIdentifiers: ['ICD', 'ORPHA'],
    sortColumn: 'name',
    sortDirection: 'asc',
    negotiatorRequestString: 'Disease type(s):',
    showFacet: true
  },
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
    showMatchTypeSelector: false,
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
    showMatchTypeSelector: false,
    negotiatorRequestString: 'Categories:',
    adaptive: false, // todo: set to true
    sortColumn: 'label',
    sortDirection: 'asc',
    showFacet: true
  },
  {
    facetTitle: 'Material type',
    component: 'CheckboxFilter',
    sourceTable: 'MaterialTypes',
    applyToColumn: 'collections.materials.name',
    filterValueAttribute: 'name',
    filterLabelAttribute: 'label',
    removeOptions: ['other'],
    showMatchTypeSelector: false,
    negotiatorRequestString: 'Material type(s):',
    adaptive: false, // todo: set to true
    sortColumn: 'label',
    sortDirection: 'asc',
    showFacet: true
  },
  {
    facetTitle: 'Collaboration type',
    component: 'CheckboxFilter',
    applyToColumn: 'collections.availableForCommercialUse',
    showMatchTypeSelector: false,
    customOptions: [{ text:  'Commercial use', value: true }, { text: 'Non-commercial use only', value: false }],
    negotiatorRequestString: 'Biobank collaboration type(s):',
    showFacet: true
  },

  {
    facetTitle: 'Biobank services',
    component: 'CheckboxFilter',
    sourceTable: 'Capabilities',
    applyToColumn: 'capabilities.name',
    filterValueAttribute: 'name',
    filterLabelAttribute: 'label',
    negotiatorRequestString: 'Biobank services:',
    sortColumn: 'label',
    sortDirection: 'asc',
    showFacet: true
  },
  {
    facetTitle: 'Quality labels',
    component: 'CheckboxFilter',
    sourceTable: 'AssessmentLevels',
    applyToColumn: 'collections.combinedQualityInfo.name',
    filterValueAttribute: 'name',
    filterLabelAttribute: 'label',
    negotiatorRequestString: 'Quality label(s):',
    sortColumn: 'label',
    sortDirection: 'asc',
    showFacet: true
  },
  {
    facetTitle: 'Network',
    component: 'CheckboxFilter',
    sourceTable: 'Networks',
    applyToColumn: 'collections.combinedNetwork.id',
    filterValueAttribute: 'id',
    filterLabelAttribute: 'name',
    negotiatorRequestString: 'Network(s):',
    sortColumn: 'name',
    sortDirection: 'asc',
    showFacet: true
  },
  {
    facetTitle: 'Data category',
    component: 'CheckboxFilter',
    sourceTable: 'DataCategories',
    applyToColumn: 'collections.dataCategories.name',
    filterValueAttribute: 'name',
    filterLabelAttribute: 'label',
    removeOptions: ['other'],
    negotiatorRequestString: 'Data type(s):',
    sortColumn: 'name',
    sortDirection: 'asc',
    showFacet: true
  },
  {
    facetTitle: 'search',
    component: "StringFilter",
    humanReadableString: 'Text search is',
    builtIn: true /** search should not be generated */
  }
]
