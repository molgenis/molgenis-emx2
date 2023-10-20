import { ISchemaMetaData } from "../Interfaces/IMetaData";
import { ITableMetaData } from "../Interfaces/ITableMetaData";

/**
 * @param {String} schemaName - schema where initial table is in
 * @param {String} tableName
 * @param {Object} metaData - object that contains all schema meta data
 * @param {Number} expandLevel - how many levels of grahpql should be expanded
 * @returns String of fields for use in gql query.
 * key=1 fields will always be expanded.
 * Other fields until level is reached
 */
export const columnNames = (
  schemaName: string,
  tableName: string,
  metaData: ISchemaMetaData,
  //allows expansion of ref fields to add their next layer of details.
  expandLevel: number,
  //rootLevel
  rootLevel = true
) => {
  let result = "";
  getTable(schemaName, tableName, metaData.tables)?.columns?.forEach((col) => {
    //we always expand the subfields of key=1, but other 'ref' fields only if they do not break server
    if (expandLevel > 0 || col.key == 1) {
      if (
        !rootLevel &&
        ["REF_ARRAY", "REFBACK", "ONTOLOGY_ARRAY"].includes(col.columnType)
      ) {
        //skip
      } else if (
        ["REF", "ONTOLOGY", "REF_ARRAY", "REFBACK", "ONTOLOGY_ARRAY"].includes(
          col.columnType
        )
      ) {
        result =
          result +
          " " +
          col.name +
          " {" +
          columnNames(
            col.refSchema ? col.refSchema : schemaName,
            col.refTable,
            metaData,
            //indicate that sub queries should not be expanded on ref_array, refback, ontology_array
            expandLevel - 1,
            false
          ) +
          " }";
      } else if (col.columnType === "FILE") {
        result += ` ${col.name} { id, size, extension, url }`;
      } else if (col.columnType !== "HEADING") {
        result += ` ${col.name}`;
      }
    }
  });
  return result;
};

const getTable = (
  schemaName: string,
  tableName: string,
  tableStore: ITableMetaData[]
) => {
  const result = tableStore.find(
    (table) => table.name === tableName && table.externalSchema === schemaName
  );
  return result;
};
