const state = {
  isLoading: false,
  breadCrumbs: [],
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
  resources: [],
  variableMappings: {},
  searchInput: "",
};

export default state;
