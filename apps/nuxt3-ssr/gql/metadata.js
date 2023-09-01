import gql from "graphql-tag";
export default gql`
  {
    _schema {
      name
      tables {
        name
        labels {
          locale
          value
        }
        tableType
        id
        descriptions {
          locale
          value
        }
        externalSchema
        semantics
        columns {
          name
          labels {
            locale
            value
          }
          id
          columnType
          key
          refTable
          refSchema
          refLink
          refLabel
          refLabelDefault
          refBack
          required
          readonly
          semantics
          descriptions {
            locale
            value
          }
          position
          computed
          visible
          validation
        }
      }
    }
  }
`;
