import gql from "graphql-tag";
export default gql`
  query CollectionEvent($pid: String, $name: String) {
    CollectionEvents(
      filter: {
        resource: { pid: { equals: [$pid] } }
        name: { equals: [$name] }
      }
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
      dataCategories {
        name
      }
      sampleCategories {
        name
      }
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
      coreVariables {
        name
      }
    }
  }
`;
