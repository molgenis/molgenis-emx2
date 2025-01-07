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
        description
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
          description
          position
          computed
          visible
          validation
        }
      }
    }
  }
`;
