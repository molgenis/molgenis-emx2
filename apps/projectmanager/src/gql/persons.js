import gql from "graphql-tag";
export default gql`
  {
    Persons(orderby: { name: ASC }) {
      name
      fTE
      notes
      planning {
        projectUnit {
          project {
            name
          }
          unit
        }
        startDate
        endDate
        fTE
        notes
      }
    }
  }
`;
