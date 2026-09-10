import { chartQuery } from "./UiDashboardQueries";
import type { ICharts } from "./UiDashboard";
import type { IFetchBody, IChartsResponse } from "./types";

export async function getDashboardChart(
  url: string,
  chart: string
): Promise<ICharts[]> {
  const query = chartQuery;
  const body: IFetchBody = { query: query };

  if (chart) {
    body["variables"] = { filter: { chartId: { equals: chart } } };
  }

  const response = await fetch(url, {
    method: "POST",
    body: JSON.stringify(body),
  });

  const data: IChartsResponse = await response.json();
  const charts = data.data?.Charts as ICharts[];
  return charts;
}
