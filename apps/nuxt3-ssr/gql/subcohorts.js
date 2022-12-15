import gql from "graphql-tag";
export default gql`
  query Subcohorts($pid: String) {
    Subcohorts(filter: { resource: { pid: { equals: [$pid] } } }) {
      resource {
        pid
      }
      name
      description
      numberOfParticipants
      ageGroups {
        name
        code
        order
        parent {
          name
          code
          parent {
            name
            code
          }
          parent {
            name
            code
          }
        }
      }
    }
  }
`;
