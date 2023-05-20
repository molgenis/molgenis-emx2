import gql from "graphql-tag";
export default gql`
  query Variables($filter: VariablesFilter) {
    Variables(limit: 1, filter: $filter) {
      name
      label
      description
      format {
        name
      }
      unit {
        name
      }
      dataset {
        resource {
          mg_tableclass
          id
        }
        name
      }
    }
  }
`;
