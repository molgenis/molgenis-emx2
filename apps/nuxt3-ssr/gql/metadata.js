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
        schemaId
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
          refTableId
          refSchema
          refLinkId
          refLabel
          refLabelDefault
          refBackId
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
