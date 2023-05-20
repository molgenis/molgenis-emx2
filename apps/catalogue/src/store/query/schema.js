import gql from "graphql-tag";

export default gql`
  query Schema {
    _schema {
      name
    }
  }
`;
