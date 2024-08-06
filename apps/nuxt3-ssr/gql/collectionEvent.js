import gql from "graphql-tag";
import ontologyFragment from "~~/gql/fragments/ontology";

export default gql`
  query CollectionEvent($id: String, $name: String) {
    CollectionEvents(
      filter: { collection: { id: { equals: [$id] } }, name: { equals: [$name] } }
    ) {
      collection {
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
