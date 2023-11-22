import { gql } from "graphql-request";

export default gql`
  query Cohorts($id: String) {
    Cohorts(filter: { id: { equals: [$id] } }) {
      id
      name
      contactEmail
      keywords
      acknowledgements
      fundingStatement
      designPaper {
        doi
        title
      }
      designDescription
      designSchematic {
        id
        size
        extension
        url
      }
      externalIdentifiers {
        externalIdentifierType {
          name
        }
        identifier
      }
      description
      website
      startYear
      endYear
      linkageOptions
      numberOfParticipants
      dataAccessConditionsDescription
      releaseDescription
      countries {
        name
        order
      }
      regions {
        name
        order
      }
      inclusionCriteria {
        name
        code
        order
      }
      otherInclusionCriteria
      populationDisease {
        name
        code
        order
      }
      populationOncologyTopology {
        name
        code
        order
      }
      populationOncologyMorphology {
        name
        code
        order
      }
      logo {
        url
      }
      design {
        name
      }
      type {
        name
        definition
        ontologyTermURI
      }
      collectionType {
        name
      }
      contacts {
        firstName
        lastName
        prefix
        initials
        email
        orcid
        homepage
        title {
          name
        }
        organisation {
          name
        }
        photo {
          id
          url
          size
          extension
        }
        expertise
      }
      additionalOrganisations {
        institution
      }
      dataAccessConditions {
        name
        ontologyTermURI
        code
        definition
      }
      dataUseConditions {
        name
        ontologyTermURI
        code
        definition
      }
      dataAccessFee
      documentation {
        name
        file {
          url
        }
        url
      }
      networks {
        id
        name
        description
        website
        logo {
          id
          url
          size
          extension
        }
      }
      collectionEvents {
        name
        description
        startYear {
          name
        }
        endYear {
          name
        }
        numberOfParticipants
        ageGroups {
          name
        }
        dataCategories {
          name
        }
        sampleCategories {
          name
        }
        areasOfInformation {
          name
        }
        subcohorts {
          name
        }
        coreVariables
      }
      subcohorts {
        name
        description
        numberOfParticipants
        ageGroups {
          name
          code
          order
          parent {
            name
            code
            parent {
              name
              code
            }
            parent {
              name
              code
            }
          }
        }
        mainMedicalCondition {
          name
        }
        countries {
          name
        }
        regions {
          name
        }
        inclusionCriteria
        supplementaryInformation
      }
    }
  }
`;
