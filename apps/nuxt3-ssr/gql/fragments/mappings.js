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
      collection {
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
        collection {
          id
        }
      }
    }
    targetVariable {
      dataset {
        collection {
          id
        }
        name
      }
      name
    }
  }
`;
