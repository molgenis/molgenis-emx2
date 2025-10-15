import gql from "graphql-tag";
export default gql`
  query Collections(
    $id: String
    $limit: Int
    $offset: Int
    $orderby: Collectionsorderby
  ) {
    Collections(
      limit: $limit
      offset: $offset
      filter: { partOfCollections: { id: { equals: [$id] } } }
      orderby: $orderby
    ) {
      id
      acronym
      name
      design {
        name
      }
      numberOfParticipants
    }
    Collections_agg(filter: { partOfCollections: { id: { equals: [$id] } } }) {
      count
    }
  }
`;
