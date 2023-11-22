import type { ITableMetaData, ISchemaMetaData, IColumn } from "meta-data-utils";

/**
 * @param {String} schemaId - schema where initial table is in
 * @param {String} tableId
 * @param {Object} metaData - object that contains all schema meta data
 * @param {Number} expandLevel - how many levels of grahpql should be expanded
 * @returns String of fields for use in gql query.
 * key=1 fields will always be expanded.
 * Other fields until level is reached
 */
export const getColumnIds = (
  schemaId: string,
  tableId: string,
  metaData: ISchemaMetaData,
  //allows expansion of ref fields to add their next layer of details.
  expandLevel: number,
  //rootLevel
  rootLevel = true
) => {
  let result = "";
  getTable(schemaId, tableId, metaData.tables)?.columns?.forEach((col: IColumn) => {
    //we always expand the subfields of key=1, but other 'ref' fields only if they do not break server
    if (expandLevel > 0 || col.key == 1) {
      if (
        !rootLevel &&
        ["REF_ARRAY", "REFBACK", "ONTOLOGY_ARRAY"].includes(col.columnType)
      ) {
        //skip
      } else if (["REF", "REF_ARRAY", "REFBACK"].includes(col.columnType)) {
        result =
          result +
          " " +
          col.id +
          " {" +
          getColumnIds(
            col.refSchemaId || schemaId,
            col.refTableId || tableId,
            metaData,
            //indicate that sub queries should not be expanded on ref_array, refback, ontology_array
            expandLevel - 1,
            false
          ) +
          " }";
      } else if (["ONTOLOGY", "ONTOLOGY_ARRAY"].includes(col.columnType)) {
        result = result + " " + col.id + " {name, label}";
      } else if (col.columnType === "FILE") {
        result += ` ${col.id} { id, size, extension, url }`;
      } else if (col.columnType !== "HEADING") {
        result += ` ${col.id}`;
      }
    }
  });

  return result;
};

const getTable = (
  schemaId: string,
  tableId: string,
  tableStore: ITableMetaData[]
) => {
  const result = tableStore.find(
    (table) => table.id === tableId && table.schemaId === schemaId
  );
  return result;
};
