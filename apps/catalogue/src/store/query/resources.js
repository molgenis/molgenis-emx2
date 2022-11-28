import gql from "graphql-tag";
export default gql`
  query Resources {
    Resources(orderby: { pid: ASC }) {
      pid
      name
      mg_tableclass
    }
  }
`;
