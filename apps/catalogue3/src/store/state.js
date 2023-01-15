const state = {
  isLoading: false,
  graphqlURL: "graphql",
  session: null,
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
    {
      name: "cohorts",
      conditions: [],
    },
  ],
  resources: [],
  variableMappings: {},
  searchInput: "",
};

export default state;
