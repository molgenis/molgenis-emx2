import { cmsFetch } from "../cms";
import type { IStatisticalChartData } from "../../../types/cms";

export async function getChartData(
  schema: string,
  chartId: string
): Promise<IStatisticalChartData[]> {
  const query = `query getChartData($filter: ChartDataFilter) {
        ChartData(filter: $filter) {
            id
        }
    }`;
  const variables = {
    filter: { displayedInChart: { id: { equals: chartId } } },
  };
  const { data } = await cmsFetch(schema, query, variables);
  return data?.ChartData as IStatisticalChartData[];
}
