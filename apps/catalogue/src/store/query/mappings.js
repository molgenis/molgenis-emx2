import gql from "graphql-tag";
export default gql`
  query VariableMappings($filter: VariableMappingsFilter) {
    VariableMappings(limit: 200, filter: $filter) {
      fromTable {
        dataDictionary {
          resource {
            pid
          }
          version
        }
        name
      }
      toVariable {
        table {
          dataDictionary {
            resource {
              pid
            }
            version
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
