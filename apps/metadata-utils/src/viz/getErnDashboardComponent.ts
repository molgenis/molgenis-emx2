import type { IComponents } from "./ErnDashboard";
import type { IFetchBody, IComponentsResponse } from "./types";

export async function getComponentStats(
  url: string,
  component?: string
): Promise<IComponents[]> {
  const query = `query getComponents($filter: ComponentsFilter) {
    Components(filter: $filter) {
      name
      label
      statistics(orderby: [{valueOrder: ASC}]) {
        id
        value
        label
        valueOrder
        description
      }
    }
  }
  `;

  const body: IFetchBody = { query: query };
  if (component) {
    body["variables"] = { filter: { name: { equals: component } } };
  }

  const response = await fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(body),
  });

  const data: IComponentsResponse = await response.json();
  return data.data?.Components as IComponents[];
}
