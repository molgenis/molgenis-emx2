import gql from "graphql-tag";
export default gql`
  query CollectionEvents(
    $id: String
    $limit: Int
    $offset: Int
    $orderby: CollectionEventsorderby
  ) {
    CollectionEvents(
      limit: $limit
      offset: $offset
      filter: { collection: { id: { equals: [$id] } } }
      orderby: $orderby
    ) {
      collection {
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
    CollectionEvents_agg(filter: { collection: { id: { equals: [$id] } } }) {
      count
    }
  }
`;
