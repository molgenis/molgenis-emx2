import gql from "graphql-tag";
export default gql`
  query CollectionSubcohorts(
    $id: String
    $limit: Int
    $offset: Int
    $orderby: CollectionSubcohortsorderby
  ) {
    CollectionSubcohorts(
      limit: $limit
      offset: $offset
      filter: { collection: { id: { equals: [$id] } } }
      orderby: $orderby
    ) {
      collection {
        id
      }
      name
      description
      numberOfParticipants
    }
    CollectionSubcohorts_agg(
      filter: { collection: { id: { equals: [$id] } } }
    ) {
      count
    }
  }
`;
