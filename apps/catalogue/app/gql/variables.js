import gql from "graphql-tag";
export default gql`
  query Variables(
    $limit: Int
    $offset: Int
    $orderby: Variablesorderby
    $filter: VariablesFilter
    $search: String
  ) {
    Variables(
      limit: $limit
      offset: $offset
      filter: $filter
      orderby: $orderby
      search: $search
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
    Variables_agg(filter: $filter, search: $search) {
      count
    }
  }
`;
