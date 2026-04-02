import type { IColumn, ITableMetaData } from "../../../metadata-utils/src";
import type {
  columnValue,
  columnValueObject,
  fileValue,
  IRefColumn,
  IRow,
} from "../../../metadata-utils/src/types";
import { executeExpression } from "../../../molgenis-components/src/components/forms/formUtils/formUtils";

export function getInitialFormValues(metadata: ITableMetaData) {
  return metadata.columns.reduce(
    (accum: Record<string, any>, column: IColumn) => {
      if (column.defaultValue !== undefined) {
        if (column.defaultValue.startsWith("=")) {
          try {
            accum[column.id] = executeExpression(
              `(${column.defaultValue.substr(1)})`,
              {},
              metadata
            );
          } catch (error) {
            console.error(
              `Default value expression failed for column ${column.id}: ${error}`
            );
          }
        } else if (column.columnType === "BOOL") {
          accum[column.id] = getBooleanValue(column.defaultValue);
        } else {
          accum[column.id] = column.defaultValue;
        }
      }
      return accum;
    },
    {}
  );
}

function getBooleanValue(value: any): boolean | undefined {
  if (value === "TRUE" || value === "true" || value === true) {
    return true;
  } else if (value === "FALSE" || value === "false" || value === false) {
    return false;
  } else {
    return undefined;
  }
}

export function getOntologyArrayValues(val: any): string[] {
  return Array.isArray(val)
    ? val
        .filter((value: columnValueObject) => value)
        .map((value: columnValueObject) => value["name"] as string)
    : [];
}

export function assertStringValue(
  value: columnValue
): string | undefined | null {
  if (typeof value !== "string" && value !== null && value !== undefined) {
    throw new Error(`Expected a string value, but got ${typeof value}`);
  }
  return value;
}

export function assertNumberValue(
  value: columnValue
): number | undefined | null {
  if (typeof value === "string") {
    const num = Number(value);
    if (isNaN(num)) {
      throw new Error(`Expected a number value, but got ${typeof value}`);
    }
    return num;
  }

  if (typeof value !== "number" && value !== null && value !== undefined) {
    throw new Error(`Expected a number value, but got ${typeof value}`);
  }
  return value;
}

export function assertBooleanValue(
  value: columnValue
): boolean | undefined | null {
  if (typeof value !== "boolean" && value !== null && value !== undefined) {
    throw new Error(`Expected a boolean value, but got ${typeof value}`);
  }
  return value;
}

export function assertRowValue(value: columnValue): IRow | undefined | null {
  if (typeof value !== "object" || value === null || Array.isArray(value)) {
    throw new Error(`Expected an object value, but got ${typeof value}`);
  }
  return value;
}

export function assertTableValue(
  value: columnValue
): IRow[] | undefined | null {
  if (value === null || value === undefined) {
    return value;
  }
  if (!Array.isArray(value)) {
    throw new Error(`Expected an array value, but got ${typeof value}`);
  }
  value.forEach(assertRowValue);
  return value as IRow[];
}

export function assertFileValue(
  value: columnValue
): fileValue | undefined | null {
  if (typeof value !== "object" || value === null || Array.isArray(value)) {
    throw new Error(`Expected an object value, but got ${typeof value}`);
  }
  // allow empty objects to be treated as undefined file values :S
  if (isEmptyObject(value)) {
    return undefined;
  }
  if (!("filename" in value) || typeof value.filename !== "string") {
    throw new Error(`Expected an object with a string 'filename' property`);
  }
  return value as fileValue;
}

export function assertListValue(
  value: columnValue
): columnValue[] | undefined | null {
  if (value === null || value === undefined) {
    return value;
  }
  if (!Array.isArray(value)) {
    throw new Error(`Expected an array value, but got ${typeof value}`);
  }
  return value as columnValue[];
}

export function assertRefColumn(column: IColumn): asserts column is IRefColumn {
  if (
    !(
      column.refTableId &&
      typeof column.refTableId === "string" &&
      column.refSchemaId &&
      typeof column.refSchemaId === "string"
    )
  ) {
    throw new Error("Column is not a valid reference column");
  }
}

export function assertRefColumnValue(
  column: columnValue
): asserts column is IRow {
  if (
    column == null ||
    Array.isArray(column) ||
    typeof column !== "object" ||
    Object.getPrototypeOf(column) !== Object.prototype
  ) {
    throw new Error("Value is not a valid reference column value");
  }
}

export function toRefColumn(column: IColumn): IRefColumn {
  assertRefColumn(column);
  return column;
}

export function toRefColumnValue(column: columnValue): IRow {
  assertRefColumnValue(column);
  return column;
}

function isEmptyObject(column: columnValue) {
  return (
    column &&
    typeof column === "object" &&
    !Array.isArray(column) &&
    Object.keys(column).length === 0
  );
}
