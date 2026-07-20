import type { IDashboardPages } from "./UiDashboard";
import type { IFetchBody, IDashboardPagesResponse } from "./types";

export async function getDashboardPage(
  url: string,
  page?: string
): Promise<IDashboardPages[]> {
  const query = `query getDashboardPage($filter: DashboardPagesFilter) {
        DashboardPages(filter: $filter) {
          name
          description
          charts {
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
      }
    `;

  const body: IFetchBody = { query: query };
  if (page) {
    body["variables"] = { filter: { name: { equals: page } } };
  }

  const response = await fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(body),
  });
  const data: IDashboardPagesResponse = await response.json();
  const pages = data.data?.DashboardPages as IDashboardPages[];
  return pages;
}
