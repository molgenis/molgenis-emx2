import gql from "graphql-tag";
export default gql`
  query Datasets($id: String, $name: String) {
    Datasets(
      filter: { resource: { id: { equals: [$id] } }, name: { equals: [$name] } }
    ) {
      name
      description
      resource {
        id
      }
      name
      label
      unitOfObservation {
        name
        code
      }
      keywords {
        name
        code
      }
      description
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
