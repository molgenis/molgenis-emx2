import { createError } from "#app";
import { fetchMetadata } from "#imports";
import type { columnValue } from "../../metadata-utils/src/types";
import { type IQueryMetaData } from "../../molgenis-components/src/client/IQueryMetaData";

export interface ITableDataResponse {
  rows: Record<string, columnValue>[];
  count: number;
}

export default async (
  schemaId: string,
  tableId: string,
  properties?: IQueryMetaData
): Promise<ITableDataResponse> => {
  const limit = properties?.limit ? properties?.limit : 20;
  const offset = properties?.offset ? properties?.offset : 0;
  const expandLevel =
    properties?.expandLevel || properties?.expandLevel == 0
      ? properties?.expandLevel
      : 2;
  const search = properties?.searchTerms
    ? ',search:"' + properties?.searchTerms.trim() + '"'
    : "";

  const columnIds = await getColumnIds(schemaId, tableId, expandLevel);
  const query = `query ${tableId}( $filter:${tableId}Filter, $orderby:${tableId}orderby ) {
        ${tableId}(
          filter:$filter,
          limit:${limit}, 
          offset:${offset}${search},
          orderby:$orderby
          )
          {
            ${columnIds}
          }
          ${tableId}_agg( filter:$filter${search} ) {
            count
          }
        }`;

  const filter = properties?.filter ? properties?.filter : {};
  const orderby = properties?.orderby ? properties?.orderby : {};

  const { data } = await $fetch(`/${schemaId}/graphql`, {
    method: "POST",
    body: {
      query,
      variables: { filter, orderby },
    },
  }).catch((error) => {
    const message = `Could not fetch data for table ${tableId} in schema ${schemaId}`;
    console.error(message, error);
    throw createError({
      ...error,
      statusMessage: message,
    });
  });

  console.log(`Fetching data for table ${tableId} schema ${schemaId}`);

  return { rows: data[tableId], count: data[`${tableId}_agg`].count };
};

export const getColumnIds = async (
  schemaId: string,
  tableId: string,
  //allows expansion of ref fields to add their next layer of details.
  expandLevel: number,
  //rootLevel
  rootLevel = true
) => {
  const metaData = await fetchMetadata(schemaId);

  const columns =
    metaData.tables.find((table) => table.id === tableId)?.columns || [];

  let gqlFields = "";
  for (const col of columns) {
    //we always expand the subfields of key, but other 'ref' fields only if they do not break server
    if (expandLevel > 0 || col.key) {
      if (
        !rootLevel &&
        ["REF_ARRAY", "REFBACK", "ONTOLOGY_ARRAY"].includes(col.columnType)
      ) {
        //skip
      } else if (["REF", "REF_ARRAY", "REFBACK"].includes(col.columnType)) {
        gqlFields =
          gqlFields +
          " " +
          col.id +
          " {" +
          (await getColumnIds(
            col.refSchemaId || schemaId,
            col.refTableId || tableId,
            //indicate that sub queries should not be expanded on ref_array, refback, ontology_array
            expandLevel - 1,
            false
          )) +
          " }";
      } else if (["ONTOLOGY", "ONTOLOGY_ARRAY"].includes(col.columnType)) {
        gqlFields = gqlFields + " " + col.id + " {name, label}";
      } else if (col.columnType === "FILE") {
        gqlFields += ` ${col.id} { id, size, filename, extension, url }`;
      } else if (col.columnType !== "HEADING") {
        gqlFields += ` ${col.id}`;
      }
    }
  }

  return gqlFields;
};
