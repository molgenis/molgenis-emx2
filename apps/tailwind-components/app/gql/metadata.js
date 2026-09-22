import gql from "graphql-tag";
export default gql`
  {
    _schema {
      id
      label
      tables {
        id
        schemaId
        name
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
          cascadeDelete
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
