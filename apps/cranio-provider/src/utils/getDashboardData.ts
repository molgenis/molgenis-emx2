import { request, gql } from "graphql-request";
import type {
  IDashboardPagesResponse,
  IChartsResponse,
  ICharts,
  IDashboardPages,
} from "../types/schema";

export async function getDashboardPage(
  url: string,
  page: string
): Promise<IDashboardPages[]> {
  const query = gql`
    query getDashboard($filter: DashboardPagesFilter) {
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
          dataPoints {
            dataPointId
            dataPointName
            dataPointValue
            dataPointValueLabel
            dataPointSeries
            dataPointPrimaryCategory
            dataPointSecondaryCategory
            dataPointPrimaryCategoryLabel
            dataPointSecondaryCategoryLabel
            dataPointTime
            dataPointTimeUnit
            dataPointColor
            dataPointDescription
            dataPointOrder
          }
          dashboardPage {
            name
          }
        }
      }
    }
  `;
  const variables = { filter: { name: { equals: page } } };
  const response: IDashboardPagesResponse = await request(
    url,
    query,
    variables
  );
  return response.DashboardPages;
}

export async function getDashboardChart(
  url: string,
  chart: string
): Promise<ICharts[]> {
  const query = gql`
    query getChart($filter: ChartsFilter) {
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
        dataPoints {
          dataPointId
          dataPointName
          dataPointValue
          dataPointValueLabel
          dataPointSeries
          dataPointPrimaryCategory
          dataPointSecondaryCategory
          dataPointPrimaryCategoryLabel
          dataPointSecondaryCategoryLabel
          dataPointTime
          dataPointTimeUnit
          dataPointColor
          dataPointDescription
          dataPointOrder
        }
        dashboardPage {
          name
        }
      }
    }
  `;
  const variables = { filter: { chartId: { equals: chart } } };
  const response: IChartsResponse = await request(url, query, variables);
  return response.Charts;
}
