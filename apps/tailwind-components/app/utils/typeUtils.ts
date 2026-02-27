import type { IColumn, ITableMetaData } from "../../../metadata-utils/src";
import type {
  columnValue,
  columnValueObject,
} from "../../../metadata-utils/src/types";
import { executeExpression } from "../../../molgenis-components/src/components/forms/formUtils/formUtils";

export function getInitialFormValues(metadata: ITableMetaData) {
  return metadata.columns.reduce(
    (accum: Record<string, columnValue>, column: IColumn) => {
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

function getBooleanValue(
  value: string | boolean | undefined
): boolean | undefined {
  if (value === "TRUE" || value === "true" || value === true) {
    return true;
  } else if (value === "FALSE" || value === "false" || value === false) {
    return false;
  } else {
    return undefined;
  }
}

export function getOntologyArrayValues(val: columnValue): string[] {
  return Array.isArray(val)
    ? val
        .filter((value: columnValueObject) => value)
        .map((value: columnValueObject) => value["name"] as string)
    : [];
}
