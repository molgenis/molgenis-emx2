import gql from "graphql-tag";
export default gql`
  query Variables($name: String) {
    Variables(filter: { name: { equals: [$name] } }) {
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
    RepeatedVariables_agg(
      filter: { isRepeatOf: { name: { equals: [$name] } } }
    ) {
      count
    }
  }
`;
