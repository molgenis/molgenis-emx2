import gql from "graphql-tag";
export default gql`
  query Subcohorts($id: String) {
    Subcohorts(filter: { resource: { id: { equals: [$id] } } }) {
      resource {
        id
      }
      name
      description
      numberOfParticipants
    }
  }
`;
