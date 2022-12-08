import gql from "graphql-tag";
export default gql`
  query Subcohorts($pid: String) {
    Cohorts(filter: { pid: { equals: [$pid] } }) {
      name
      subcohorts {
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
  }
`;
