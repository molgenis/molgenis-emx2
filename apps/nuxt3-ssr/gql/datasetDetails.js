import gql from "graphql-tag";
export default gql`
  query ResourceDatasets($resource: String, $name: String) {
    ResourceDatasets(
      filter: {
        resource: { id: { equals: [$resource] } }
        name: { equals: [$name] }
      }
    ) {
      resource {
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
