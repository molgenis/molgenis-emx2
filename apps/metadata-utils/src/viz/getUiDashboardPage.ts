import { dashboardQuery } from "./UiDashboardQueries";
import type { IDashboardPages } from "./UiDashboard";
import type { IFetchBody, IDashboardPagesResponse } from "./types";

export async function getDashboardPage(
  url: string,
  page?: string
): Promise<IDashboardPages[]> {
  const query = dashboardQuery;
  const body: IFetchBody = { query: query };

  if (page) {
    body["variables"] = { filter: { name: { equals: page } } };
  }

  const response = await fetch(url, {
    method: "POST",
    body: JSON.stringify(body),
  });

  const data: IDashboardPagesResponse = await response.json();
  const pages = data.data?.DashboardPages as IDashboardPages[];
  return pages;
}
