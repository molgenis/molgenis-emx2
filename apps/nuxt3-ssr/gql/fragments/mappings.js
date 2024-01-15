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
  }
`;
