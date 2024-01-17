import gql from "graphql-tag";
import mappingsFragment from "~~/gql/fragments/mappings";
export default gql`
  query Variables(
    $variableFilter:VariablesFilter,
    $cohortsFilter:CohortsFilter,
    ) {
    Variables(filter: $variableFilter) {
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
    Cohorts(orderby: { id: ASC }, filter: $cohortsFilter) {
      id
      name
    }
    RepeatedVariables_agg(filter: { isRepeatOf: $variableFilter }) {
      count
    }
  }
`;
