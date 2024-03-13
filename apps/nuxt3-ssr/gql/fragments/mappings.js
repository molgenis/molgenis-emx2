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
      mg_tableclass
    }
    sourceVariablesOtherDatasets {
      name
      dataset {
        name
        resource {
          id
        }
      }
      mg_tableclass
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
