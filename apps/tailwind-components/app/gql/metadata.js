import gql from "graphql-tag";
export default gql`
  {
    _schema {
      id
      label
      tables {
        id
        schemaId
        label
        tableType
        description
        semantics
        columns {
          id
          label
          formLabel
          section
          heading
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
          defaultValue
          position
          computed
          visible
          validation
        }
      }
    }
  }
`;
