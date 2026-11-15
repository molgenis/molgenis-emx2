import { cmsFetch } from "../cms";
import type { IStatisticalChartData, IFiles } from "../../../types/cms";

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

export async function getFiles(
  schema: string,
  fileTag: string,
  orderby:
    | "fileTag"
    | "alternateFileName"
    | "file"
    | "linkToExternalFile" = "fileTag",
  direction: "ASC" | "DESC" = "ASC"
): Promise<IFiles[]> {
  const query = `query getFiles($filter:FilesFilter, $orderby:[Filesorderby]) {
    Files(filter:$filter,orderby:$orderby) {
      alternateFileName
      file {
         id
         size
         filename
         extension
         url
      }
      fileTag
      linkToExternalFile
    }
  }`;

  let variables = {
    filter: {},
    orderby: [{ [orderby]: direction }],
  };
  if (fileTag !== "") {
    variables.filter = { fileTag: { equals: fileTag } };
  }

  const url: string = `/${schema}/graphql`;
  const response: any = await $fetch(url, {
    method: "POST",
    body: { query: query, variables: variables },
  });
  return response.data?.Files ?? [];
}
