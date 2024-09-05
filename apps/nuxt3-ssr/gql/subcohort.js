import gql from "graphql-tag";
export default gql`
  query CollectionSubcohort($id: String, $name: String) {
    CollectionSubcohorts(
      filter: {
        collection: { id: { equals: [$id] } }
        name: { equals: [$name] }
      }
    ) {
      collection {
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
