import gql from "graphql-tag";
export default gql`
  query Datasets($id: String) {
    CollectionDatasets(filter: { collection: { id: { equals: [$id] } } }) {
      name
      description
    }
    CollectionDatasets_agg(filter: { collection: { id: { equals: [$id] } } }) {
      count
    }
  }
`;
