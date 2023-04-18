import gql from "graphql-tag";
export default gql`
  query Subcohorts($id: String, $limit: Int, $offset: Int) {
    Subcohorts(
      limit: $limit
      offset: $offset
      filter: { resource: { id: { equals: [$id] } } }
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
