import gql from "graphql-tag";
import mappingsFragment from "~~/gql/fragments/mappings";
export default gql`
  query Variables($filter: VariablesFilter) {
    Variables(filter: $filter) {
      name
      resource {
        name
      }
      dataset {
        name
        resource {
          id
        }
      }
      label
      description
      unit {
        name
      }
      format {
        name
      }
      mappings ${moduleToString(mappingsFragment)}
      repeats {
        name
        mappings ${moduleToString(mappingsFragment)}
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
