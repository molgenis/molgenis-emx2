import { gql } from "graphql-tag";

export default gql`
  query DataSources(
    $id: String
    $limit: Int
    $offset: Int
    $orderby: DataSourcesorderby
  ) {
    DataSources(
      limit: $limit
      offset: $offset
      filter: { networks: { id: { equals: [$id] } } }
      orderby: $orderby
    ) {
      id
      acronym
      name
      type {
        name
      }
      numberOfParticipants
    }
    DataSources_agg(filter: { networks: { id: { equals: [$id] } } }) {
      count
    }
  }
`;
