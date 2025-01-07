import gql from "graphql-tag";
import mappingsFragment from "~~/gql/fragments/mappings";
export default gql`
  query Variables(
    $variableFilter:VariablesFilter,
    $resourcesFilter:ResourcesFilter,
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
      repeatUnit {
        name
      }
      repeatMin
      repeatMax
      format {
        name
      }
      mappings ${moduleToString(mappingsFragment)}
    }
    Resources(orderby: { id: ASC }, filter: $resourcesFilter) {
      id
      name
    }
  }
`;
