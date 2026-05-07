import { moduleToString } from "../../../tailwind-components/app/utils/moduleToString";
import fileFragment from "./fragments/file";
import ontologyFragment from "./fragments/ontology";

export const resourceQuery = `
  query Resources($id: String) {
    Resources(filter: { id: { equals: [$id] } }) {
      id
      pid
      acronym
      name
      description
      website
      logo {
        url
      }
      contactEmail
      type {
        name
      }
      cohortType {
        name
      }
      registryOrHealthRecordType {
        name
      }
      networkType {
        name
      }
      clinicalStudyType {
        name
      }
      keywords
      externalIdentifiers {
        identifier
        externalIdentifierType{name}
      }
      populationAgeGroups {
        name order code parent { code }
      }
      dateLastRefresh
      startYear
      endYear
      continents {
        name
        order
      }
      countries {
        name order
      }
      regions {
        name
        order
      }
      numberOfParticipants
      numberOfParticipantsWithSamples
      designDescription
      designSchematic ${moduleToString(fileFragment)}
      design {
        definition
        name
      }
      dataCollectionType {
        definition
        name
      }
      dataCollectionDescription
      reasonSustained
      unitOfObservation
      recordTrigger
      populationOncologyTopology ${moduleToString(ontologyFragment)}
      populationOncologyMorphology ${moduleToString(ontologyFragment)}
      inclusionCriteria ${moduleToString(ontologyFragment)}
      otherInclusionCriteria
      exclusionCriteria ${moduleToString(ontologyFragment)}
      otherExclusionCriteria
      publications(orderby: {title:ASC}) {
        doi
        title
        isDesignPublication
      }
      collectionEvents {
        name
        description
        startDate
        endDate
        numberOfParticipants
        ageGroups ${moduleToString(ontologyFragment)}
        dataCategories ${moduleToString(ontologyFragment)}
        sampleCategories ${moduleToString(ontologyFragment)}
        areasOfInformation ${moduleToString(ontologyFragment)}
        subpopulations {
          name
        }
        coreVariables
      }
      peopleInvolved {
        roleDescription
        firstName
        lastName
        prefix
        initials
        email
        title {
          name
        }
        organisation {
          name
          website
          organisationWebsite
          email
          organisation {
            name
            label
          }
      }
        role ${moduleToString(ontologyFragment)}
      }
      organisationsInvolved(orderby: {name: ASC})  {
        id
        name
        website
        isLeadOrganisation
        role ${moduleToString(ontologyFragment)}
        otherOrganisation
        organisation {
          name
          acronym
          website
          country {
            name
            order
          }
        }
      }
      subpopulations {
          name
          mainMedicalCondition ${moduleToString(ontologyFragment)}
      }
      dataAccessConditions ${moduleToString(ontologyFragment)}
      dataAccessConditionsDescription
      dataUseConditions ${moduleToString(ontologyFragment)}
      dataAccessFee
      releaseType ${moduleToString(ontologyFragment)}
      releaseDescription
      fundingStatement
      acknowledgements
      linkageOptions
      prelinked
      documentation {
        name
        description
        url
        file ${moduleToString(fileFragment)}
      }
      datasets {
        name
      }
      partOfNetworks {
        id
        name
        description
        website
        logo {
          url
        }
        catalogueType {
          name
        }
      }
      publications_agg {
        count
      }
      subpopulations_agg {
        count
      }
      collectionEvents_agg{
        count
      }
    }
    Variables_agg(filter: { resource: { id: { equals: [$id] } } }) {
      count
    }
  }
`;
