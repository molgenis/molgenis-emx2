import { gql } from "graphql-request";

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
      filter: { resource: { id: { equals: [$id] } } }
      orderby: $orderby
    ) {
      resource {
        id
      }
      name
      description
      numberOfParticipants
    }
    Subcohorts_agg(filter: { resource: { id: { equals: [$id] } } }) {
      count
    }
  }
`;
