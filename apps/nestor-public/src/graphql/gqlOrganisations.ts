import gql from "graphql-tag";

export default gql`
  query Organisations {
    name
    code
    city
    country
    latitude
    longitude
    providerInformation {
      providerIdentifier
      hasSubmittedData
    }
  }
`;
