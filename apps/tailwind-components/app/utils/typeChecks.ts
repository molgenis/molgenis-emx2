import type { IColumn } from "../../../metadata-utils/src";
import type { fileValue, IRefColumn } from "../../../metadata-utils/src/types";

export function isStringElement(
  type: string | undefined,
  listElement: any
): listElement is string {
  return (
    type === "STRING" ||
    type === "AUTO_ID" ||
    type === "PERIOD" ||
    type === "DATE" ||
    type === "DATETIME" ||
    type === "UUID" ||
    type === "LONG"
  );
}

export function isTextElement(
  type: string | undefined,
  listElement: any
): listElement is string {
  return type === "TEXT";
}

export function isEmailElement(
  type: string | undefined,
  listElement: any
): listElement is string {
  return type === "EMAIL";
}

export function isHyperlinkElement(
  type: string | undefined,
  listElement: any
): listElement is string {
  return type === "HYPERLINK";
}

export function isBoolElement(
  type: string | undefined,
  listElement: any
): listElement is boolean {
  return type === "BOOL";
}

export function isDecimalElement(
  type: string | undefined,
  listElement: any
): listElement is number {
  return type === "DECIMAL";
}

export function isIntElement(
  type: string | undefined,
  listElement: any
): listElement is number {
  return type === "INT" || type === "NON_NEGATIVE_INT";
}

export function isObjectElement(
  type: string | undefined,
  listElement: any
): listElement is Record<string, any> {
  return (
    type === "REF" ||
    type === "RADIO" ||
    type === "SELECT" ||
    type === "MULTISELECT" ||
    type === "CHECKBOX" ||
    type === "ONTOLOGY"
  );
}

export function isFileElement(
  type: string | undefined,
  listElement: any
): listElement is fileValue {
  return type === "FILE";
}

export function isRefColumn(column: IColumn): column is IRefColumn {
  return (
    column.columnType === "REF" ||
    column.columnType === "RADIO" ||
    column.columnType === "SELECT"
  );
}

export function isRefBackColumn(column: IColumn): column is IRefColumn {
  return column.columnType === "REFBACK";
}

export function isListElement(
  type: string | undefined,
  listElement: any
): listElement is any[] {
  return (
    type?.endsWith("ARRAY") || type === "MULTISELECT" || type === "CHECKBOX"
  );
}
