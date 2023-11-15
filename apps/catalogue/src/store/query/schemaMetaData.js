import gql from "graphql-tag";
export default gql`
  {
    _schema {
      label
      tables {
        label
        id
        description
        schemaId
        semantics
        columns {
          label
          id
          columnType
          key
          refTableId
          refLinkId
          refLabel
          refBackId
          required
          semantics
          description
          position
        }
      }
    }
  }
`;
