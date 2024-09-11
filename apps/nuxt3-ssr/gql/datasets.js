import gql from "graphql-tag";
export default gql`
  query Datasets($id: String) {
    Datasets(filter: { resource: { id: { equals: [$id] } } }) {
      name
      description
    }
    Datasets_agg(filter: { resource: { id: { equals: [$id] } } }) {
      count
    }
  }
`;
