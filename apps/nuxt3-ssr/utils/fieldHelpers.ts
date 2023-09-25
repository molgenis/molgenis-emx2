import { IColumn } from "interfaces/types";

export const fieldTypes = () => {
  return [
    "BOOL",
    "BOOL_ARRAY",
    "DATE",
    "DATE_ARRAY",
    "DATETIME",
    "AUTO_ID",
    "DATETIME_ARRAY",
    "DECIMAL",
    "DECIMAL_ARRAY",
    "EMAIL",
    "EMAIL_ARRAY",
    "FILE",
    "HEADING",
    "HYPERLINK",
    "HYPERLINK_ARRAY",
    "INT",
    "INT_ARRAY",
    "LONG",
    "LONG_ARRAY",
    "ONTOLOGY",
    "ONTOLOGY_ARRAY",
    "REF",
    "REF_ARRAY",
    "REFBACK",
    "STRING",
    "STRING_ARRAY",
    "TEXT",
    "TEXT_ARRAY",
    "UUID",
    "UUID_ARRAY",
  ];
};

export const isEmpty = (obj: object) => {
  for (const prop in obj) {
    if (Object.hasOwn(obj, prop)) {
      return false;
    }
  }

  return true;
};

export const isValueType = (column: IColumn) => {
  return (
    column.columnType === "STRING" ||
    column.columnType === "TEXT" ||
    column.columnType === "EMAIL" ||
    column.columnType === "HYPERLINK" ||
    column.columnType === "UUID" ||
    column.columnType === "DATE" ||
    column.columnType === "DATETIME" ||
    column.columnType === "INT" ||
    column.columnType === "LONG" ||
    column.columnType === "DECIMAL"
  );
};

export const isRefType = (column: IColumn) => {
  return (
    column.columnType === "REF" ||
    column.columnType === "REFBACK" ||
    column.columnType === "ONTOLOGY"
  );
};

export const isArrayType = (column: IColumn) => {
  return column.columnType.endsWith("_ARRAY");
};

export const isFileType = (column: IColumn) => {
  return column.columnType === "FILE";
};
