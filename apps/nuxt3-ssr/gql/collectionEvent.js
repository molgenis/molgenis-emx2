import gql from "graphql-tag";
import ontologyFragment from "~~/gql/fragments/ontology";

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
      ageGroups ${moduleToString(ontologyFragment)}
      dataCategories ${moduleToString(ontologyFragment)}
      sampleCategories ${moduleToString(ontologyFragment)}
      standardizedTools ${moduleToString(ontologyFragment)}
      standardizedToolsOther
      areasOfInformation ${moduleToString(ontologyFragment)}
      subcohorts {
        name
      }
      coreVariables
    }
  }
`;
