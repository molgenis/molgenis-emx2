import gql from "graphql-tag";
export default gql`
  query Publications($id: String) {
    Publications(filter: { equals: { doi: $id } }) {
      doi
      title
      authors
      year
      journal
      volume
      number
      pagination
      publisher
      school
      abstract
    }
  }
`;
