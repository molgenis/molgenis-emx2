import type { ICharts } from "./UiDashboard";
import type { IFetchBody, IChartsResponse } from "./types";

export async function getDashboardChart(
  url: string,
  chart: string
): Promise<ICharts[]> {
  const query = `query getChart($filter: ChartsFilter) {
        Charts(filter: $filter) {
          chartId
          chartType {
            name
          }
          chartTitle
          chartSubtitle
          xAxisLabel
          xAxisMinValue
          xAxisMaxValue
          xAxisTicks
          yAxisLabel
          yAxisMinValue
          yAxisMaxValue
          yAxisTicks
          topMargin
          rightMargin
          bottomMargin
          leftMargin
          legendPosition
          dataPoints(
            orderby: [
              { primaryCategory: ASC }
              { secondaryCategory: ASC }
              { sortOrder: ASC }
            ]
          ) {
            id
            name
            value
            valueLabel
            series
            primaryCategory
            secondaryCategory
            primaryCategoryLabel
            secondaryCategoryLabel
            timeValue
            timeUnit
            color
            description
            sortOrder
          }
        }
      }
    `;

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
