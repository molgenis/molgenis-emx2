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
          refTable
          refLink
          refLabel
          refBack
          required
          semantics
          description
          position
        }
      }
    }
  }
`;
