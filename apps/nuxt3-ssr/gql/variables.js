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
      resource {
        id
      }
    }
    Variables_agg(filter: $filter) {
      count
    }
  }
`;
