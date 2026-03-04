import gql from "graphql-tag";
export default gql`
  query Subpopulations($id: String, $name: String) {
    Subpopulations(
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
      inclusionCriteria {
        name
        order
      }
      otherInclusionCriteria
      exclusionCriteria {
        name
        order
      }
      otherExclusionCriteria
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
