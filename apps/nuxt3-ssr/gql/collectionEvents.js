import gql from "graphql-tag";
export default gql`
  query ResourceCollectionEvents(
    $id: String
    $limit: Int
    $offset: Int
    $orderby: ResourceCollectionEventsorderby
  ) {
    ResourceCollectionEvents(
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
      startDate
      endDate
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
    ResourceCollectionEvents_agg(
      filter: { resource: { id: { equals: [$id] } } }
    ) {
      count
    }
  }
`;
