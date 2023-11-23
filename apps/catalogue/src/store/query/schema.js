import { gql } from "graphql-request";

export default gql`
  query Schema {
    _schema {
      name
    }
  }
`;
