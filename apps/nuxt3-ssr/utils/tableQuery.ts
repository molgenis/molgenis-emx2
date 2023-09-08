import { IColumn, ISchemaMetaData, ITableMetaData } from "interfaces/types";
import { convertToPascalCase } from "../../molgenis-components/src/components/utils";

const FILE_FRAGMENT = "{ id, size, extension, url }";

export const buildQueryFields = (
  schemas: Record<string, ISchemaMetaData>,
  schemaName: string,
  tableId: string
): string => {
  const schemaMetaData = schemas[schemaName];
  const tableMetaData = schemaMetaData.tables.find(
    (t: ITableMetaData) => t.id === tableId
  );

  const allColumns = tableMetaData?.columns;
  const dataColumns = allColumns
    ?.filter((c) => !c.id.startsWith("mg_"))
    .filter((c) => c.columnType !== "HEADING");

  const refTableQueryFields = (refColumn: IColumn): string => {
    const refTableMetaData = schemas[
      refColumn.refSchema || schemaName
    ].tables.find(
      // @ts-ignore we know that refTable is not undefined
      (t: ITableMetaData) => t.id === convertToPascalCase(refColumn.refTable)
    );

    const allRefColumns = refTableMetaData?.columns;

    const refTableDataColumns = allRefColumns
      ?.filter((c) => !c.id.startsWith("mg_"))
      .filter((c) => c.columnType !== "HEADING");

    const refFields = refTableDataColumns?.map((column) => {
      switch (column.columnType) {
        case "STRING":
        case "TEXT":
          return column.id;
        case "FILE":
          return `${column.id} ${FILE_FRAGMENT}`;
        case "REF":
        case "ONTOLOGY":
        case "REF_ARRAY":
        case "REFBACK":
        case "ONTOLOGY_ARRAY":
          return ""; // stop recursion
        default:
          return column.id;
      }
    });

    const refQueryFields = refFields ? refFields.join(" ") : "";

    return refQueryFields;
  };

  const fields = dataColumns?.map((column) => {
    switch (column.columnType) {
      case "STRING":
      case "TEXT":
        return column.id;
      case "FILE":
        return `${column.id} ${FILE_FRAGMENT}`;
      case "REF":
      case "ONTOLOGY":
      case "REF_ARRAY":
      case "REFBACK":
      case "ONTOLOGY_ARRAY":
        return `${column.id} { ${refTableQueryFields(column)} }`;
      default:
        return column.id;
    }
  });

  const queryFields = fields ? fields.join(" ") : "";

  return queryFields;
};

export const extractExternalSchemas = (schemaMetaData: ISchemaMetaData) => {
  return [
    ...new Set(
      schemaMetaData.tables.reduce((acc: string[], table: ITableMetaData) => {
        table.columns.forEach((column: IColumn) => {
          if (column.refSchema) {
            acc.push(column.refSchema);
          }
        });
        return acc;
      }, [])
    ),
  ];
};
