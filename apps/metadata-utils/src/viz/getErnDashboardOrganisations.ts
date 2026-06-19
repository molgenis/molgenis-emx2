import type { IOrganisations } from "./ErnDashboard";
import type { IFetchBody, IOrganisationsResponse } from "./types";

export async function getOrganisations(
  url: string,
  organisation?: string
): Promise<IOrganisations[]> {
  const query = `query getOrganisations($filter: OrganisationsFilter) {
        Organisations(filter: $filter) {
            name
            code
            city
            country
            latitude
            longitude
            organisationType
            providerInformation {
                providerIdentifier
                hasSubmittedData
            }
        } 
    }`;

  const body: IFetchBody = { query: query };
  if (organisation) {
    body["variables"] = { filter: { name: { equals: organisation } } };
  }

  const response = await fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(body),
  });
  const data: IOrganisationsResponse = await response.json();
  return data.data?.Organisations as IOrganisations[];
}
