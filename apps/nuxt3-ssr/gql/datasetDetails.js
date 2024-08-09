import gql from "graphql-tag";
export default gql`
  query Datasets($id: String, $name: String) {
    Datasets(
      filter: {
        collection: { id: { equals: [$id] } }
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
