import gql from "graphql-tag";
export default gql`
  query Subcohort($id: String, $name: String) {
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
        order
        name
        code
        parent {
          code
        }
      }
      mainMedicalCondition {
        name
        code
        order
        definition
        ontologyTermURI
        parent {
          name
        }
        children {
          name
        }
      }
      countries {
        name
        order
      }
      regions {
        name
        order
      }
      inclusionCriteria
      supplementaryInformation
      comorbidity {
        name
        code
        order
        definition
        ontologyTermURI
        parent {
          name
        }
        children {
          name
        }
      }
    }
  }
`;
