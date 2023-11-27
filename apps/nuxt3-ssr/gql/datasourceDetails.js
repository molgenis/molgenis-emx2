import gql from "graphql-tag";

export default gql`
  query dataSource($id: String) {
    DataSources(filter: { id: { equals: [$id] } }) {
      id
      acronym
      name
      description
      website
      keywords
      leadOrganisation {
        id
        name
      }
      type {
        name
      }
      dateEstablished
      startDataCollection
      logo {
        url
      }
      numberOfParticipants
      countries {
        name
        order
        code
        parent {
          code
        }
      }
      populationAgeGroups {
        name
        order
        code
        parent {
          code
        }
      }
      populationEntry {
        name
        order
        code
        parent {
          code
        }
      }
      populationExitOther
      populationDisease {
        name
        order
        code
        parent {
          code
        }
      }
      datasets {
        resource {
          id
          pid
          acronym
          name
          website
          description
          logo {
            id
            size
            extension
            url
          }
          fundingStatement
          acknowledgements
        }
        name
        label
        unitOfObservation {
          order
          name
          label
          parent {
            name
          }
          codesystem
          code
          ontologyTermURI
          definition
        }
        keywords {
          order
          name
          label
          parent {
            name
          }
          codesystem
          code
          ontologyTermURI
          definition
        }
        description
        numberOfRows
        mappedTo {
          source {
            id
          }
          sourceDataset {
            resource {
              id
            }
            name
          }
          target {
            id
          }
          targetDataset {
            resource {
              id
            }
            name
          }
          order
          description
          syntax
        }
        mappedFrom {
          source {
            id
          }
          sourceDataset {
            resource {
              id
            }
            name
          }
          target {
            id
          }
          targetDataset {
            resource {
              id
            }
            name
          }
          order
          description
          syntax
        }
        sinceVersion
        untilVersion
      }
      areasOfInformation {
        name
        order
        code
        parent {
          code
        }
      }
      qualityOfLifeOther
      languages {
        name
      }
      recordTrigger
      linkagePossibility
      linkageDescription
      linkedResources {
        preLinked
        otherLinkedResource
        linkedResource {
          id
        }
        mainResource {
          localName
        }
      }
    }
  }
`;
