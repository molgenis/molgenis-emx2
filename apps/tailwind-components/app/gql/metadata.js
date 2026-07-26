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
        inheritId
        columns {
          id
          label
          formLabel
          section
          heading
          columnType
          inherited
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
          role
          display
          validation
        }
      }
    }
  }
`;
