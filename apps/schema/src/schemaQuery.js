import gql from "graphql-tag";

export default gql`
  {
    _session {
      schemas
      roles
    }
    _schema {
      name
      tables {
        name
        tableType
        inherit
        externalSchema
        labels {
          locale
          value
        }
        descriptions {
          locale
          value
        }
        semantics
        columns {
          id
          name
          labels {
            locale
            value
          }
          table
          position
          columnType
          inherited
          key
          refSchema
          refTable
          refLink
          refBack
          refLabel
          required
          readonly
          defaultValue
          descriptions {
            locale
            value
          }
          semantics
          validation
          visible
          computed
        }
      }
    }
  }
`;
