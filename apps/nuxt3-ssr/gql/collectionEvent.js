import { gql } from "graphql-tag";

export default gql`
  query CollectionEvent($id: String, $name: String) {
    CollectionEvents(
      filter: { resource: { id: { equals: [$id] } }, name: { equals: [$name] } }
    ) {
      resource {
        name
      }
      name
      description
      startYear {
        name
      }
      startMonth {
        name
      }
      endYear {
        name
      }
      endMonth {
        name
      }
      numberOfParticipants
      ageGroups {
        name
        order
        code
        parent {
          code
        }
      }
      dataCategories {
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
      sampleCategories {
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
      standardizedTools {
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
      standardizedToolsOther
      areasOfInformation {
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
      subcohorts {
        name
      }
      coreVariables
    }
  }
`;
