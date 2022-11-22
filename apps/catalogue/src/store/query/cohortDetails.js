import gql from "graphql-tag";
export default gql`
  query Cohorts($pid: String) {
    Cohorts(filter: { pid: { equals: [$pid] } }) {
      pid
      name
      contact_email
      keywords
      acknowledgements
      funding_statement
      design_paper {
        doi
        title
      }
      design_description
      design_schematic {
        id
        size
        extension
        url
      }
      external_identifiers{
        external_identifier_type{
          name
        },
        identifier
      }
      description
      website
      start_year
      end_year
      linkage_options
      number_of_participants
      data_access_conditions_description
      release_description
      countries {
        name
        order
      }
      regions {
        name
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
        ontology_term_URI
      }
      collection_type {
        name
      }
      contributors {
        contact {
          first_mame
          last_name
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
        contribution_type {
          name
          order
        }
        contribution_description
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
