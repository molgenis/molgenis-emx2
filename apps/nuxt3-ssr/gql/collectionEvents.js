import { gql } from "graphql-request";

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
      filter: { resource: { id: { equals: [$id] } } }
      orderby: $orderby
    ) {
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
    CollectionEvents_agg(filter: { resource: { id: { equals: [$id] } } }) {
      count
    }
  }
`;
