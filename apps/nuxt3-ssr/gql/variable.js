import gql from "graphql-tag";
export default gql`
  query Variables($filter: VariablesFilter) {
    Variables(filter: $filter) {
      name
      label
      description
      unit {
        name
      }
      format {
        name
      }
      mappings {
        syntax
        description
        source {
          id
          name
        }
        match {
          name
        }
        sourceDataset {
          resource {
            id
          }
        }
      }
      repeats {
        name
        mappings {
          syntax
          description
          source {
            id
            name
          }
          match {
            name
          }
          sourceDataset {
            resource {
              id
            }
          }
        }
      }
    }
    Cohorts(orderby: { id: ASC }) {
      id
      name
    }
    RepeatedVariables_agg(filter: { isRepeatOf: $filter }) {
      count
    }
  }
`;
