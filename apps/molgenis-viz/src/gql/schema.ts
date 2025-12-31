import gql from "graphql-tag";

/**
 * Query for
 */
export default gql`
  query {
    _schema {
      name
      tables {
        name
        tableType
        columns {
          name
          label
          columnType
        }
      }
    }
  }
`;
