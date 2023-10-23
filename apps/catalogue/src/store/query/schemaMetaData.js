import gql from "graphql-tag";
export default gql`
  {
    _schema {
      name
      tables {
        name
        id
        description
        externalSchema
        semantics
        columns {
          name
          id
          columnType
          key
          refTableName
          refTableId
          refLinkName
          refLinkId
          refLabel
          refBackId
          refBackName
          required
          semantics
          description
          position
        }
      }
    }
  }
`;
