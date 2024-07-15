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
      id: "keywords",
      conditions: [],
    },
    {
      id: "networks",
      conditions: [],
    },
    {
      id: "cohorts",
      conditions: [],
    },
  ],
  resources: [],
  cohorts: [],
  variableMappings: {},
  searchInput: "",
};

export default state;
