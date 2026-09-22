import gql from "graphql-tag";
export default gql`
  query Tables(
    $id: String
    $limit: Int
    $offset: Int
    $orderby: [Tablesorderby]
  ) {
    Tables(
      filter: { resource: { id: { equals: [$id] } } }
      limit: $limit
      offset: $offset
      orderby: $orderby
    ) {
      name
      description
    }
    Tables_agg(filter: { resource: { id: { equals: [$id] } } }) {
      count
    }
  }
`;
