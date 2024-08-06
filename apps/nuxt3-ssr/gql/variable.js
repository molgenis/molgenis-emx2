import gql from "graphql-tag";
import mappingsFragment from "~~/gql/fragments/mappings";
export default gql`
  query Variables(
    $variableFilter:VariablesFilter,
    $collectionsFilter:CollectionsFilter,
    ) {
    Variables(filter: $variableFilter) {
      name
      collection {
        name
      }
      dataset {
        name
        collection {
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
    }
    Collections(orderby: { id: ASC }, filter: $collectionsFilter) {
      id
      name
    }
  }
`;
