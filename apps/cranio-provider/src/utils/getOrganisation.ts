// import gql from "graphql-tag";
import { request, gql } from "graphql-request";
import type {
  IOrganisations,
  IOrganisationsResponse,
} from "../interfaces/schema";

export async function getOrganisation(
  url: string,
  organisationId: string
): Promise<IOrganisations> {
  const query = gql`query {
      Organisations(filter: {
      providerInformation: {
        providerIdentifier: {
          equals: "${organisationId}"
        }
      }
    }) {
        name
        code
        city
        country
        latitude
        longitude
        image {
          filename
          extension
          url
        }
        providerInformation {
          providerIdentifier
        }
      }
    }
  `;

  const result: IOrganisationsResponse = await request(url, query, {
    providerInformation: {
      providerIdentifier: { equals: `${organisationId}` },
    },
  });

  return result.Organisations[0];
}
