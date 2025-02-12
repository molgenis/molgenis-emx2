import type { ITableMetaData } from "metadata-utils";
import { fetchSchemaMetaData } from "./client";

/**
 * @param {String} schemaId - schema where initial table is in
 * @param {String} tableId
 * @param {Number} expandLevel - how many levels of grahpql should be expanded
 * @returns String of fields for use in gql query.
 * key=1 fields will always be expanded.
 * Other fields until level is reached
 */
export const getColumnIds = async (
  schemaId: string,
  tableId: string,
  expandLevel: number,
  rootLevel = true
) => {
  const metadata = await fetchSchemaMetaData(schemaId);
  let result = "";
  for (const col of getColumns(schemaId, tableId, metadata.tables)) {
    //we always expand the subfields of key, but other 'ref' fields only if they do not break server
    if (expandLevel > 0 || col.key) {
      if (
        !rootLevel &&
        ["REF_ARRAY", "REFBACK", "ONTOLOGY_ARRAY"].includes(col.columnType)
      ) {
        continue;
      } else if (["REF", "REF_ARRAY", "REFBACK"].includes(col.columnType)) {
        result =
          result +
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
        result = result + " " + col.id + " {name, label}";
      } else if (col.columnType === "FILE") {
        result += ` ${col.id} { id, size, filename, extension, url }`;
      } else if (col.columnType !== "HEADING") {
        result += ` ${col.id}`;
      }
    }
  }

  return result;
};

const getColumns = (
  schemaId: string,
  tableId: string,
  tableStore: ITableMetaData[]
) => {
  const result = tableStore.find((table) => table.id === tableId);
  return result?.columns || [];
};
