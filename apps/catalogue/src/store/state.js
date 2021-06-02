const state = {
  isLoading: false,
  variables: [],
  variableDetails: {},
  variableCount: undefined,
  keywords: [],
  filters: [
    {
      name: "keywords",
      conditions: [],
    },
  ],
  selectedKeywords: [],
  cohorts: [],
  variableMappings: [],
  searchInput: "",
};

export default state;
