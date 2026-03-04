import gql from "graphql-tag";
export default gql`
  query Datasets(
    $id: String
    $limit: Int
    $offset: Int
    $orderby: Datasetsorderby
  ) {
    Datasets(
      filter: { resource: { id: { equals: [$id] } } }
      limit: $limit
      offset: $offset
      orderby: $orderby
    ) {
      name
      description
    }
    Datasets_agg(filter: { resource: { id: { equals: [$id] } } }) {
      count
    }
  }
`;
