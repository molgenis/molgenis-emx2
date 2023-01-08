import gql from "graphql-tag";
export default gql`
  query CollectionEvents($pid: String) {
    CollectionEvents(filter: { resource: { pid: { equals: [$pid] } } }) {
      resource {
        pid
      }
      name
      description
      startYear {
        name
      }
      endYear {
        name
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
      numberOfParticipants
    }
  }
`;
