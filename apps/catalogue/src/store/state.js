const state = {
  isLoading: false,
  schema: null,
  variables: [],
  variableDetails: {},
  variableCount: undefined,
  keywords: [],
  filters: [
    {
      name: "keywords",
      conditions: [],
    },
    {
      name: "networks",
      conditions: [],
    },
  ],
  cohorts: [],
  variableMappings: [],
  searchInput: "",
};

export default state;
