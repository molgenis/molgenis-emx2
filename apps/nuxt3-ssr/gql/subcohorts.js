import gql from "graphql-tag";
export default gql`
  query Subcohorts(
    $id: String
    $limit: Int
    $offset: Int
    $orderby: Subcohortsorderby
  ) {
    Subcohorts(
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
    Subcohorts_agg(filter: { collection: { id: { equals: [$id] } } }) {
      count
    }
  }
`;
