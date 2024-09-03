import gql from "graphql-tag";
import ontologyFragment from "~~/gql/fragments/ontology";

export default gql`
  query ResourceCollectionEvent($id: String, $name: String) {
    ResourceCollectionEvents(
      filter: { resource: { id: { equals: [$id] } }, name: { equals: [$name] } }
    ) {
      resource {
        name
      }
      name
      description
      startDate
      endDate
      numberOfParticipants
      ageGroups ${moduleToString(ontologyFragment)}
      dataCategories ${moduleToString(ontologyFragment)}
      sampleCategories ${moduleToString(ontologyFragment)}
      standardizedTools ${moduleToString(ontologyFragment)}
      standardizedToolsOther
      areasOfInformation ${moduleToString(ontologyFragment)}
      cohorts {
        name
      }
      coreVariables
    }
  }
`;
