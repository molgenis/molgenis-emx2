import gql from "graphql-tag";
export default gql`
  query Cohorts(
    $id: String
    $limit: Int
    $offset: Int
    $orderby: Cohortsorderby
  ) {
    Cohorts(
      limit: $limit
      offset: $offset
      filter: { networks: { id: { equals: [$id] } } }
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
    Cohorts_agg(filter: { networks: { id: { equals: [$id] } } }) {
      count
    }
  }
`;
