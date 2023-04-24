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
        source {
          id
        }
        sourceVariables {
          name
        }
        sourceVariablesOtherDatasets {
          dataset {
            name
          }
          name
        }
        sourceDataset {
          resource {
            id
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
          source {
            id
          }
          sourceVariables {
            name
          }
          sourceDataset {
            resource {
              id
            }
            name
          }
        }
      }
    }
  }
`;
