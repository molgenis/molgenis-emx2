import type { IOrganisations } from "../../../metadata-utils/src/viz/ErnDashboard";

export async function getOrganisation(
  url: string,
  organisationId: string
): Promise<IOrganisations> {
  const query = `query getOrganisations($filter: OrganisationsFilter){
      Organisations(filter:$filter) {
        name
        code
        city
        country
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
  }`;
  const response = await fetch(url, {
    method: "POST",
    body: JSON.stringify({
      query: query,
      variables: {
        filter: {
          providerInformation: {
            providerIdentifier: { equals: `${organisationId}` },
          },
        },
      },
    }),
  });

  const responseJson = await response.json();
  return responseJson.data.Organisations[0];
}
