import gql from "graphql-tag";
export default gql`
  query Variables(
    $limit: Int
    $offset: Int
    $orderby: Variablesorderby
    $filter: VariablesFilter
  ) {
    Variables(
      limit: $limit
      offset: $offset
      filter: $filter
      orderby: $orderby
    ) {
      name
      label
      description
      unit {
        name
      }
      format {
        name
      }
      resource {
        id
      }
      dataset {
        name
        resource {
          id
        }
      }
    }
    Variables_agg(filter: $filter) {
      count
    }
  }
`;
