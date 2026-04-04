import gql from "graphql-tag";

/**
 * Query for
 */
export default gql`
  query {
    _schema(applyProfileFilter: true) {
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
