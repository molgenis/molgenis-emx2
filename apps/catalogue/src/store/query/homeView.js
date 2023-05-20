import gql from "graphql-tag";
export default gql`
  query {
    _schema {
      name
    }
    _settings(keys: ["newsItems", "recentlyAdded"]) {
      key
      value
    }
    Institutions_agg {
      count
    }
    Studies_agg {
      count
    }
    Cohorts_agg {
      count
    }
    Databanks_agg {
      count
    }
    Cohorts {
      numberOfParticipants
      design {
        name
      }
      collectionType {
        name
      }
      collectionEvents {
        dataCategories {
          name
        }
        sampleCategories {
          name
        }
      }
    }
  }
`;
