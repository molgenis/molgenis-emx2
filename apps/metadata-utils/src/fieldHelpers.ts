import type { ColumnType, IColumn } from "./types";

const SINGLE_REF_TYPES = new Set<string>([
  "REF",
  "SELECT",
  "RADIO",
] satisfies ColumnType[]);

const REF_ARRAY_TYPES = new Set<string>([
  "REF_ARRAY",
  "CHECKBOX",
  "MULTISELECT",
] satisfies ColumnType[]);

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
    "SECTION",
    "HYPERLINK",
    "HYPERLINK_ARRAY",
    "INT",
    "INT_ARRAY",
    "NON_NEGATIVE_INT",
    "NON_NEGATIVE_INT_ARRAY",
    "LONG",
    "LONG_ARRAY",
    "ONTOLOGY",
    "ONTOLOGY_ARRAY",
    "REF",
    "REF_ARRAY",
    "SELECT",
    "MULTISELECT",
    "RADIO",
    "CHECKBOX",
    "REFBACK",
    "STRING",
    "STRING_ARRAY",
    "TEXT",
    "TEXT_ARRAY",
    "UUID",
    "UUID_ARRAY",
    "JSON",
  ];
};

export const isEmpty = (obj: object) => {
  for (const prop in obj) {
    if (Object.hasOwnProperty.call(obj, prop)) {
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
    column.columnType === "NON_NEGATIVE_INT" ||
    column.columnType === "LONG" ||
    column.columnType === "DECIMAL" ||
    column.columnType === "JSON"
  );
};

export const isRefType = (column: IColumn) => {
  return (
    column.columnType === "REF" ||
    column.columnType === "REFBACK" ||
    column.columnType === "ONTOLOGY" ||
    column.columnType === "SELECT" ||
    column.columnType === "RADIO"
  );
};

export const isArrayType = (column: IColumn) => {
  return column.columnType.endsWith("_ARRAY");
};

export const isSingleRefType = (columnType: string): boolean =>
  SINGLE_REF_TYPES.has(columnType);

export const isRefbackType = (columnType: string): boolean =>
  columnType === "REFBACK";

export const isSingleOntologyType = (columnType: string): boolean =>
  columnType === "ONTOLOGY";

const isCollectionType = (columnType: string): boolean =>
  REF_ARRAY_TYPES.has(columnType) || isRefbackType(columnType);

const isMultiValuedType = (columnType: string): boolean =>
  columnType.endsWith("_ARRAY") || isCollectionType(columnType);

/**
 * Multi-valued and stored on the row itself, so the values arrive with the
 * record. A REFBACK is multi-valued too, but it is derived from the other
 * table, so it is fetched and rendered separately.
 */
export const isStoredMultiValuedType = (columnType: string): boolean =>
  isMultiValuedType(columnType) && !isRefbackType(columnType);

export const isFileType = (columnType: string): boolean =>
  columnType === "FILE";
