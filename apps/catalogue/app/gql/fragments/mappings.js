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
    sourceDataset {
      resource {
        id
      }
    }
    sourceVariables {
      name
      resource {
        id
      }
      dataset {
        name
        resource {
          id
        }
      }
    }
    sourceVariablesOtherDatasets {
      name
      dataset {
        name
        resource {
          id
        }
      }
    }
    targetVariable {
      dataset {
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
