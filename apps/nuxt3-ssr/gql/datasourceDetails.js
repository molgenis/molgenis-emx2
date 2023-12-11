import gql from "graphql-tag";
import ontologyFragment from "~~/gql/fragments/ontology";
import fileFragment from "~~/gql/fragments/file";

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
      type ${moduleToString(ontologyFragment)}
      dateEstablished
      startDataCollection
      logo ${moduleToString(fileFragment)}
      numberOfParticipants
      countries ${moduleToString(ontologyFragment)}
      populationAgeGroups ${moduleToString(ontologyFragment)}
      populationEntry ${moduleToString(ontologyFragment)}
      populationExitOther
      populationDisease ${moduleToString(ontologyFragment)}
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
        unitOfObservation ${moduleToString(ontologyFragment)}
        keywords ${moduleToString(ontologyFragment)}
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
      areasOfInformation ${moduleToString(ontologyFragment)}
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
      dataHolder {
        id
        pid
        acronym
        name
        type {
          name
        }
      }
      dAPs {
        populationSubsetOther
        isDataAccessProvider {
          name
        }
        organisation {
          id
        }
        resource {
          id
        }
      }
      informedConsent ${moduleToString(ontologyFragment)}
      informedConsentOther
      accessIdentifiableData
      accessIdentifiableDataRoute
      accessSubjectDetails
      accessSubjectDetailsRoute
      auditPossible
      standardOperatingProcedures
      biospecimenAccess
      biospecimenAccessConditions
      governanceDetails
      approvalForPublication
      preservation
      preservationDuration
      refreshPeriod ${moduleToString(ontologyFragment)}
      dateLastRefresh
      qualification
      qualificationsDescription
      accessForValidation
      qualityValidationFrequency
      qualityValidationMethods
      correctionMethods
      qualityValidationResults
      cdms {
        source {
          id
          name
        }

        sourceVersion
        target {
          id
        }
        mappingStatus ${moduleToString(ontologyFragment)}
        eTLFrequency
      }
      cdmsOther
      designPaper {
        doi
        title
      }
      publications {
        doi
        title
      }
      informedConsentType ${moduleToString(ontologyFragment)}
      fundingSources {
        name
      }
      fundingStatement
      acknowledgements
      documentation {
        name
        description
      }
      supplementaryInformation
      networks {
        id
        name
      }
      studies {
        id
        name
      }
    }
  }
`;
