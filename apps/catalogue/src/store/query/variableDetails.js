import gql from "graphql-tag";
export default gql`
  query TargetVariables($filter: TargetVariablesFilter) {
    TargetVariables(limit: 1, filter: $filter) {
      name
      label
      description
      format {
        name
      }
      unit {
        name
      }
      permittedValues {
        value
        label
        order
        isMissing
        ontologyTermURI
      }
      mappings {
        syntax
        description
        match {
          name
        }
        fromDataDictionary {
          resource {
            pid
          }
        }
        fromVariable {
          name
        }
        fromVariablesOtherTables {
          table {
            name
          }
          name
        }
        fromTable {
          dataDictionary {
            resource {
              pid
            }
            version
          }
          name
        }
      }
      repeats {
        name
        mappings {
          syntax
          description
          match {
            name
          }
          fromDataDictionary {
            resource {
              pid
            }
          }
          fromVariable {
            name
          }
          fromTable {
            dataDictionary {
              resource {
                pid
              }
              version
            }
            name
          }
        }
      }
    }
  }
`;
