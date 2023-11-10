import gql from "graphql-tag";
export default gql`
  {
    _schema {
      id
      tables {
        name
        label
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
          refSchemaId
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
