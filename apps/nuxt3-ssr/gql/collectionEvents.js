import gql from "graphql-tag";
export default gql`
  query Subcohorts($pid: String) {
    Cohorts(filter: { pid: { equals: [$pid] } }) {
      name
      collectionEvents {
        name
        description
        startYear {
          name
        }
        endYear {
          name
        }
      }
    }
  }
`;
