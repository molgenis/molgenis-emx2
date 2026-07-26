import type { ColumnType } from "./types";

const ALL_COLUMN_TYPES: Record<ColumnType, true> = {
  BOOL: true,
  BOOL_ARRAY: true,
  UUID: true,
  UUID_ARRAY: true,
  FILE: true,
  STRING: true,
  STRING_ARRAY: true,
  TEXT: true,
  TEXT_ARRAY: true,
  INT: true,
  INT_ARRAY: true,
  LONG: true,
  LONG_ARRAY: true,
  DECIMAL: true,
  DECIMAL_ARRAY: true,
  DATE: true,
  DATE_ARRAY: true,
  DATETIME: true,
  DATETIME_ARRAY: true,
  PERIOD: true,
  PERIOD_ARRAY: true,
  JSON: true,
  REF: true,
  REF_ARRAY: true,
  REFBACK: true,
  PARTS: true,
  RADIO: true,
  SELECT: true,
  HEADING: true,
  SECTION: true,
  AUTO_ID: true,
  ONTOLOGY: true,
  ONTOLOGY_ARRAY: true,
  EMAIL: true,
  EMAIL_ARRAY: true,
  HYPERLINK: true,
  HYPERLINK_ARRAY: true,
  NON_NEGATIVE_INT: true,
  NON_NEGATIVE_INT_ARRAY: true,
  CHECKBOX: true,
  MULTISELECT: true,
};

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

const OPTION_KEY_REF_TYPES = new Set<string>([
  "RADIO",
  "CHECKBOX",
] satisfies ColumnType[]);

const REFBACK_TYPES = new Set<string>([
  "REFBACK",
  "PARTS",
] satisfies ColumnType[]);

const ONTOLOGY_TYPES = new Set<string>([
  "ONTOLOGY",
  "ONTOLOGY_ARRAY",
] satisfies ColumnType[]);

const LAYOUT_COLUMN_TYPES = new Set<string>([
  "HEADING",
  "SECTION",
] satisfies ColumnType[]);

const VALUE_TYPES = new Set<string>([
  "AUTO_ID",
  "BOOL",
  "BOOL_ARRAY",
  "DATE",
  "DATE_ARRAY",
  "DATETIME",
  "DATETIME_ARRAY",
  "DECIMAL",
  "DECIMAL_ARRAY",
  "EMAIL",
  "EMAIL_ARRAY",
  "HYPERLINK",
  "HYPERLINK_ARRAY",
  "INT",
  "INT_ARRAY",
  "JSON",
  "LONG",
  "LONG_ARRAY",
  "NON_NEGATIVE_INT",
  "NON_NEGATIVE_INT_ARRAY",
  "PERIOD",
  "PERIOD_ARRAY",
  "STRING",
  "STRING_ARRAY",
  "TEXT",
  "TEXT_ARRAY",
  "UUID",
  "UUID_ARRAY",
] satisfies ColumnType[]);

export const fieldTypes = (): string[] => Object.keys(ALL_COLUMN_TYPES);

export const isSingleRefType = (columnType: string): boolean =>
  SINGLE_REF_TYPES.has(columnType);

export const isRefArrayType = (columnType: string): boolean =>
  REF_ARRAY_TYPES.has(columnType);

export const isOptionKeyRefType = (columnType: string): boolean =>
  OPTION_KEY_REF_TYPES.has(columnType);

export const isRefbackType = (columnType: string): boolean =>
  REFBACK_TYPES.has(columnType);

export const isOntologyType = (columnType: string): boolean =>
  ONTOLOGY_TYPES.has(columnType);

export const isPartsType = (columnType: string): boolean =>
  columnType === "PARTS";

export const isOntologyArrayType = (columnType: string): boolean =>
  columnType === "ONTOLOGY_ARRAY";

export const isSingleOntologyType = (columnType: string): boolean =>
  columnType === "ONTOLOGY";

export const isAutoIdType = (columnType: string): boolean =>
  columnType === "AUTO_ID";

export const isRefType = (columnType: string): boolean =>
  isSingleRefType(columnType) ||
  isRefArrayType(columnType) ||
  isRefbackType(columnType) ||
  isOntologyType(columnType);

export const isCollectionType = (columnType: string): boolean =>
  isRefArrayType(columnType) || isRefbackType(columnType);

export const isTableRefType = (columnType: string): boolean =>
  isSingleRefType(columnType) || isCollectionType(columnType);

export const isStoredTableRefType = (columnType: string): boolean =>
  isSingleRefType(columnType) || isRefArrayType(columnType);

export const isMultiValuedRefType = (columnType: string): boolean =>
  isCollectionType(columnType) || isOntologyArrayType(columnType);

export const isLayoutColumnType = (columnType: string): boolean =>
  LAYOUT_COLUMN_TYPES.has(columnType);

export const isSectionType = (columnType: string): boolean =>
  columnType === "SECTION";

export const isPlainHeadingType = (columnType: string): boolean =>
  columnType === "HEADING";

export const isValueType = (columnType: string): boolean =>
  VALUE_TYPES.has(columnType);

export const isArrayType = (columnType: string): boolean =>
  columnType.endsWith("_ARRAY");

export const isValueArrayType = (columnType: string): boolean =>
  isValueType(columnType) && isArrayType(columnType);

export const isMultiValuedType = (columnType: string): boolean =>
  isArrayType(columnType) || isCollectionType(columnType);

export const isStoredMultiValuedType = (columnType: string): boolean =>
  isMultiValuedType(columnType) && !isRefbackType(columnType);

export const isFileType = (columnType: string): boolean =>
  columnType === "FILE";

export const isEmpty = (obj: object) => {
  for (const prop in obj) {
    if (Object.hasOwnProperty.call(obj, prop)) {
      return false;
    }
  }

  return true;
};
