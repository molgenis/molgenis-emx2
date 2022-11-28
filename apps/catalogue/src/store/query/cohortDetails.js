import gql from "graphql-tag";
export default gql`
  query Cohorts($pid: String) {
    Cohorts(filter: { pid: { equals: [$pid] } }) {
      pid
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
      externalIdentifiers
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
      populationAgeGroups {
        name
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
      contributors {
        contact {
          firstName
          surname
          prefix
          initials
          department
          email
          orcid
          homepage
          title {
            name
          }
          institution {
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
        contributionType {
          name
          order
        }
        contributionDescription
      }
      partners {
        institution {
          name
          pid
          logo {
            id
            url
            size
            extension
          }
        }
        role {
          name
        }
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
      institution {
        pid
        name
      }
      documentation {
        name
        file {
          url
        }
        url
      }
      networks {
        pid
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
        coreVariables {
          name
        }
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
