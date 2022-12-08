import gql from "graphql-tag";
export default gql`
  query SourceVariables($filter: SourceVariablesFilter) {
    SourceVariables(limit: 1, filter: $filter) {
      name
      label
      description
      format {
        name
      }
      unit {
        name
      }
      dataDictionary {
        resource {
          mg_tableclass
          pid
        }
        version
      }
    }
  }
`;
