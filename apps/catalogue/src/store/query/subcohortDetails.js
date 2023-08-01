import gql from "graphql-tag";

export default gql`
  query Subcohorts($id: String, $name: String) {
    Subcohorts(
      filter: { resource: { id: { equals: [$id] } }, name: { equals: [$name] } }
    ) {
      resource {
        name
      }
      name
      description
      numberOfParticipants
      inclusionStart
      inclusionEnd
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
      mainMedicalCondition {
        name
      }
      countries {
        name
      }
      regions {
        name
      }
      inclusionCriteria
      supplementaryInformation
      comorbidity {
        name
      }
      counts {
        ageGroup {
          name
        }
        nFemale
        nMale
        nTotal
      }
    }
  }
`;
