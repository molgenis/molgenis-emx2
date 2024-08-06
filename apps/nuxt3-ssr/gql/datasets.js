import gql from "graphql-tag";
export default gql`
  query Datasets($id: String) {
    Datasets(filter: { collection: { id: { equals: [$id] } } }) {
      name
      description
    }
    Datasets_agg(filter: { collection: { id: { equals: [$id] } } }) {
      count
    }
  }
`;
