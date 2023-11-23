import { gql } from "graphql-tag";
export default gql`
  query Cohorts($id: String) {
    Cohorts(filter: { id: { equals: [$id] } }) {
      acronym
      name
      description
      website
      contactEmail
      leadOrganisation {
        acronym
      }
      type {
        name
      }
      collectionType {
        name
      }
      populationAgeGroups {
        name
        order
        code
        parent {
          code
        }
      }
      startYear
      endYear
      countries {
        name
        order
      }
      regions {
        name
        order
      }
      numberOfParticipants
      numberOfParticipantsWithSamples
      designDescription
      design {
        definition
        name
      }
      designPaper {
        title
        doi
      }
      inclusionCriteria {
        name
        order
        code
        parent {
          code
        }
      }
      otherInclusionCriteria
      dataAccessConditions {
        name
        ontologyTermURI
        code
        definition
      }
      dataAccessConditionsDescription
      dataUseConditions {
        name
        ontologyTermURI
        code
        definition
      }
      dataAccessFee
      releaseDescription
      fundingStatement
      acknowledgements
    }
    CollectionEvents_agg(filter: { resource: { id: { equals: [$id] } } }) {
      count
    }
    Subcohorts_agg(filter: { resource: { id: { equals: [$id] } } }) {
      count
    }
  }
`;
