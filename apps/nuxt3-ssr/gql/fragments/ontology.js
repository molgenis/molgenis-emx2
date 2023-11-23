import { gql } from "graphql-request";

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
