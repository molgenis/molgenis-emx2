import { request, gql } from "graphql-request";
import type { IOrganisations, IOrganisationsResponse } from "../types/schema";

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
        schemaName
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
