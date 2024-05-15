import gql from "graphql-tag";
export default gql`
  query RepeatedVariables($filter: RepeatedVariablesFilter) {
    RepeatedVariables(filter: $filter) {
      name
      label
      resource {
        name
      }
      dataset {
        name
        resource {
          id
        }
      }
      isRepeatOf {
        name
        unit {
          name
        }
        format {
          name
        }
        description
      }
    }
  }
`;
