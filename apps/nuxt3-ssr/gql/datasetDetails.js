import gql from "graphql-tag";
export default gql`
  query CollectionDatasets($collection: String, $name: String) {
    CollectionDatasets(
      filter: {
        collection: { id: { equals: [$collection] } }
        name: { equals: [$name] }
      }
    ) {
      collection {
        id
      }
      name
      description
      label
      unitOfObservation {
        name
        code
      }
      keywords {
        name
        code
      }
      numberOfRows
      mappedTo {
        source {
          name
        }
        target {
          name
        }
      }
      mappedFrom {
        source {
          name
        }
        target {
          name
        }
      }
      sinceVersion
      untilVersion
    }
  }
`;
