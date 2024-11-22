import { request, gql } from "graphql-request";
import type { IDashboardPages } from "../interfaces/schema";

export async function getDashboardPage(url: string, page: string) {
  const query = gql`
    query getDashboard($filter: string) {
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
          dataPointTime
          dataPointTimeUnit
          dataPointColor
          dataPointDescription
          dataPointOrder
        }
      }
    }
  `;
  const variables = { variables: { filter: { name: { equals: page } } } };
  const response: IDashboardPages = await request(url, query, variables);
  return response;
}
