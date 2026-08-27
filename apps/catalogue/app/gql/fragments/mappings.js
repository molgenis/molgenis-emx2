import gql from "graphql-tag";
export default gql`
  {
    syntax
    description
    match {
      name
    }
    source {
      id
      name
    }
    sourceTable {
      resource {
        id
      }
    }
    sourceVariables {
      name
      resource {
        id
      }
      table {
        name
        resource {
          id
        }
      }
    }
    sourceVariablesOtherTables {
      name
      table {
        name
        resource {
          id
        }
      }
    }
    targetVariable {
      table {
        resource {
          id
        }
        name
      }
      name
    }
    repeats
  }
`;
