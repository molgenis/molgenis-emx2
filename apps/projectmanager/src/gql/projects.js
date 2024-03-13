import gql from "graphql-tag";
export default gql`
  {
    Projects(orderby: { name: ASC }) {
      name
      completed
      projectUnits(orderby: { unit: ASC }) {
        unit
        planHours
        regHoursKorade
        panama {
          regHours
        }
        planning {
          projectUnit {
            project {
              name
            }
            unit
          }
          person {
            name
          }
          startDate
          fTE
          endDate
        }
      }
    }
  }
`;
