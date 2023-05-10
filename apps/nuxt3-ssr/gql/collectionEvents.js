import gql from "graphql-tag";
export default gql`
  query CollectionEvents($id: String) {
    CollectionEvents(filter: { resource: { id: { equals: [$id] } } }) {
      resource {
        id
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
