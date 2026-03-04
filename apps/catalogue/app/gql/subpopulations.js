import gql from "graphql-tag";
export default gql`
  query Subpopulations(
    $id: String
    $limit: Int
    $offset: Int
    $orderby: Subpopulationsorderby
  ) {
    Subpopulations(
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
    Subpopulations_agg(filter: { resource: { id: { equals: [$id] } } }) {
      count
    }
  }
`;
