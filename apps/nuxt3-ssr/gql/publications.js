import gql from "graphql-tag";
export default gql`
  query Publications(
    $id: String
    $limit: Int
    $offset: Int
    $orderby: Publicationsorderby
  ) {
    Publications(
      limit: $limit
      offset: $offset
      filter: { resources: { id: { equals: [$id] } } }
      orderby: $orderby
    ) {
      resources {
        id
      }
      doi
      title
      authors
      year
      journal
      volume
      number
      pagination
      publisher
      school
      abstract
    }
    Publications_agg(filter: { resources: { id: { equals: [$id] } } }) {
      count
    }
  }
`;
