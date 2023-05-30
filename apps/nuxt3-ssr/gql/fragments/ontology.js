import gql from "graphql-tag";
export default gql`
  {
    name
    code
    order
    definition
    ontologyTermURI
    parent {
      name
      code
      order
      definition
      ontologyTermURI
      parent {
        name
        code
        order
        definition
        ontologyTermURI
        parent {
          name
          code
          order
          definition
          ontologyTermURI
          parent {
            name
            code
            order
            definition
            ontologyTermURI
            parent {
              name
              code
              order
              definition
              ontologyTermURI
              parent {
                name
                code
                order
                definition
                ontologyTermURI
                parent {
                  name
                  code
                  order
                  definition
                  ontologyTermURI
                }
              }
            }
          }
        }
      }
    }
    children {
      name
    }
  }
`;
