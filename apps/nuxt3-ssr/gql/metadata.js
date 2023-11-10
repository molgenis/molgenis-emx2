import gql from "graphql-tag";
export default gql`
  {
    _schema {
      id
      label
      tables {
        id
        label
        tableType
        descriptions {
          locale
          value
        }
        schemaId
        semantics
        columns {
          id
          label
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
