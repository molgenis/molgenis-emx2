import gql from "graphql-tag";
export default gql`
  query ResourceCohorts(
    $id: String
    $limit: Int
    $offset: Int
    $orderby: ResourceCohortsorderby
  ) {
    ResourceCohorts(
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
    ResourceCohorts_agg(filter: { resource: { id: { equals: [$id] } } }) {
      count
    }
  }
`;
