import {
  IColumn,
  ISchemaMetaData,
  ITableMetaData,
  KeyObject,
} from "~~/interfaces/types";

const FILE_FRAGMENT = "{ id, size, extension, url }";

export const buildRecordDetailsQueryFields = (
  schemas: Record<string, ISchemaMetaData>,
  schemaId: string,
  tableId: string
): string => {
  const schemaMetaData = schemas[schemaId];
  const tableMetaData = schemaMetaData.tables.find(
    (t: ITableMetaData) => t.id === tableId
  );

  const allColumns = tableMetaData?.columns;
  const dataColumns = allColumns
    ?.filter((c) => !c.id.startsWith("mg_"))
    .filter((c) => c.columnType !== "HEADING");

  const refTableQueryFields = (refColumn: IColumn): string => {
    const refTableMetaData = schemas[
      refColumn.refSchemaId || schemaId
    ].tables.find((t: ITableMetaData) => t.id === refColumn.refTableId);

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

export const buildRecordListQueryFields = (
  tableId: string,
  schemaId: string,
  schemas: Record<string, ISchemaMetaData>
) => {
  const keyFields = buildKeyFields(tableId, schemaId, schemas);
  const tableMetaData = getTableMetaData(schemas[schemaId], tableId);

  if (tableMetaData === undefined) {
    throw new Error(
      "buildRecordListQueryFields; tableMetaData is undefined for tableId " +
        tableId +
        " in schema " +
        schemaId
    );
  }

  const typeFields = tableMetaData.columns.map((c) => c.id);

  // suggested list fields that are part of this tableType
  const additionalFields: any = [
    "id",
    "label",
    "description",
    "pid",
    "acronym",
    "logo",
  ].filter((value) => typeFields.includes(value));

  // Special case for logo, expand to include all fields
  if (additionalFields[additionalFields.length - 1] === "logo") {
    additionalFields.push(["id", "size", "extension", "url"]);
  }

  const queryFields = [...new Set([...keyFields, ...additionalFields])];

  const fieldsString = fieldsToQueryString(queryFields);

  return fieldsString;
};

const fieldsToQueryString = (fields: string[][]): string => {
  return fields.reduce((acc: string, field: any) => {
    if (Array.isArray(field)) {
      return (acc += " { " + fieldsToQueryString(field) + " } ");
    } else {
      return (acc += " " + field);
    }
  }, "");
};

const buildKeyFields = (
  tableId: string,
  schemaId: string,
  schemas: Record<string, ISchemaMetaData>
) => {
  const schemaMetaData = schemas[schemaId];
  const tableMetaData = getTableMetaData(schemaMetaData, tableId);

  const keyFields = tableMetaData.columns.reduce(
    (acc: any, column: IColumn) => {
      if (column.key === 1) {
        if (isValueType(column)) {
          acc.push(column.id);
        } else if (isRefType(column)) {
          if (!column.refTableId) {
            throw new Error(
              "refTable is undefined for refColumn with id " +
                column.id +
                " in table " +
                tableId +
                ""
            );
          } else {
            acc.push(column.id);
            acc.push(
              buildKeyFields(
                column.refTableId,
                column.refSchemaId || schemaId,
                schemas
              )
            );
          }
        } else if (isArrayType(column)) {
          console.log(
            "TODO: buildRecordListQueryFields, key column isArrayType, skip for now"
          );
        } else if (isFileType(column)) {
          console.log(
            "TODO: buildRecordListQueryFields, key column isFileType, skip for now"
          );
        } else {
          console.log(
            "TODO: buildRecordListQueryFields, key column is unknown type, skip for now"
          );
        }
      }
      return acc;
    },
    []
  );
  return keyFields || [];
};

export const extractExternalSchemas = (schemaMetaData: ISchemaMetaData) => {
  return [
    ...new Set(
      schemaMetaData.tables.reduce((acc: string[], table: ITableMetaData) => {
        table.columns.forEach((column: IColumn) => {
          if (column.refSchemaId) {
            acc.push(column.refSchemaId);
          }
        });
        return acc;
      }, [])
    ),
  ];
};

export const extractKeyFromRecord = (
  record: any,
  tableId: string,
  schemaId: string,
  schemas: Record<string, ISchemaMetaData>
) => {
  const schemaMetaData = schemas[schemaId];
  const tableMetaData = getTableMetaData(schemaMetaData, tableId);

  const key = tableMetaData.columns.reduce((acc: any, column: IColumn) => {
    if (column.key === 1 && record[column.id]) {
      if (isValueType(column)) {
        acc[column.id] = record[column.id];
      } else if (isRefType(column)) {
        if (!column.refTableId) {
          throw new Error(
            "refTable is undefined for refColumn with id " +
              column.id +
              " in table " +
              tableId +
              ""
          );
        } else {
          acc[column.id] = extractKeyFromRecord(
            record[column.id],
            column.refTableId,
            column.refSchemaId || schemaId,
            schemas
          );
        }
      } else if (isArrayType(column)) {
        console.log(
          "TODO: extractKeyFromRecord, key column isArrayType, skip for now"
        );
      } else if (isFileType(column)) {
        console.log(
          "TODO: extractKeyFromRecord, key column isFileType, skip for now"
        );
      } else {
        console.log(
          "TODO: extractKeyFromRecord, key column is unknown type, skip for now"
        );
      }
    }
    return acc;
  }, {});
  return key || {};
};

export const buildFilterFromKeysObject = (keys: KeyObject) => {
  return Object.entries(keys).reduce(
    (
      acc: Record<string, object>,
      [key, value]: [string, string | KeyObject]
    ) => {
      acc[key] = { equals: [value] };
      return acc;
    },
    {}
  );
};

export const getTableMetaData = (
  schemaMetaData: ISchemaMetaData,
  tableId: string
): ITableMetaData => {
  const tableMetaData = schemaMetaData.tables.find(
    (t: ITableMetaData) => t.id === tableId
  );

  if (tableMetaData === undefined) {
    const msg = "ERROR: tableMetaData is undefined for tableId " + tableId;
    console.log(msg);
    throw new Error(msg);
  }
  return tableMetaData;
};
