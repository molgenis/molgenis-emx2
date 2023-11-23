import { gql } from "graphql-request";

export default gql`
  query Resources {
    Resources(orderby: { id: ASC }) {
      id
      name
      mg_tableclass
    }
  }
`;
