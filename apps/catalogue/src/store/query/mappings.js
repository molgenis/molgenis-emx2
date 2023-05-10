import gql from "graphql-tag";
export default gql`
  query VariableMappings($filter: VariableMappingsFilter) {
    VariableMappings(limit: 200, filter: $filter) {
      sourceDataset {
        resource {
          id
        }
        name
      }
      targetVariable {
        dataset {
          resource {
            id
          }
          name
        }
        name
      }
      match {
        name
      }
    }
  }
`;
