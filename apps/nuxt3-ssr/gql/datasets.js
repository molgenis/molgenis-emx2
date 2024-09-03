import gql from "graphql-tag";
export default gql`
  query Datasets($id: String) {
    ResourceDatasets(filter: { resource: { id: { equals: [$id] } } }) {
      name
      description
    }
    ResourceDatasets_agg(filter: { resource: { id: { equals: [$id] } } }) {
      count
    }
  }
`;
