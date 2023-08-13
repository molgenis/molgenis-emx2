import gql from "graphql-tag";
export default gql`
  query Resources {
    Resources(orderby: { id: ASC }) {
      id
      name
      mg_tableclass
    }
  }
`;
