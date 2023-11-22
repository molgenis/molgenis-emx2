import { gql } from "graphql-request";

export default gql`
  query CollectionEvents($id: String, $name: String) {
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
      }
      dataCategories {
        name
      }
      sampleCategories {
        name
      }
      areasOfInformation {
        name
      }
      subcohorts {
        name
      }
      coreVariables
    }
  }
`;
