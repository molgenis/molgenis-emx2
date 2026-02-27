import type {
  columnValue,
  IColumn,
  ITableMetaData,
} from "../../../metadata-utils/src/types";
import Client from "../client/client";
import type { IRow } from "../Interfaces/IRow";
import constants from "./constants";
import { executeExpression } from "./forms/formUtils/formUtils";

const { CODE_0, CODE_9, CODE_PERIOD, AUTO_ID } = constants;

export function isRefType(columnType: string): boolean {
  return [
    "REF",
    "REF_ARRAY",
    "REFBACK",
    "ONTOLOGY",
    "ONTOLOGY_ARRAY",
    "RADIO",
    "SELECT",
    "CHECKBOX",
    "MULTISELECT",
  ].includes(columnType);
}

export function isNumericKey(event: KeyboardEvent): boolean {
  const keyCode = event.which ?? event.keyCode;
  return (keyCode >= CODE_0 && keyCode <= CODE_9) || keyCode === CODE_PERIOD;
}

export function flattenObject(object: Record<string, unknown>): string {
  if (typeof object === "object") {
    let result = "";
    Object.keys(object).forEach((key) => {
      if (object[key] === null) {
        return;
      }
      if (typeof object[key] === "object") {
        result += flattenObject(object[key] as Record<string, unknown>);
      } else {
        result += " " + object[key];
      }
    });
    return result;
  } else {
    return String(object);
  }
}

export async function convertRowToPrimaryKey(
  row: IRow,
  tableId: string,
  schemaId?: string
): Promise<IRow> {
  const client = Client.newClient(schemaId);
  const tableMetadata = await client.fetchTableMetaData(tableId);
  if (!tableMetadata?.columns) {
    throw new Error("Empty columns in metadata");
  } else {
    return await tableMetadata.columns.reduce(
      async (accumPromise: Promise<IRow>, column: IColumn): Promise<IRow> => {
        let accum: IRow = await accumPromise;
        const cellValue = row[column.id];
        if (column.key === 1 && (cellValue || cellValue === 0)) {
          accum[column.id] = await getKeyValue(
            cellValue,
            column,
            column.refSchemaId || schemaId
          );
        }
        return accum;
      },
      Promise.resolve({})
    );
  }
}

export async function getKeyValue(
  cellValue: columnValue,
  column: IColumn,
  schemaId?: string
) {
  if (typeof cellValue === "string" || typeof cellValue === "number") {
    return cellValue;
  } else {
    if (column.refTableId) {
      return await convertRowToPrimaryKey(
        cellValue as IRow,
        column.refTableId,
        schemaId
      );
    } else {
      throw new Error("Unexpected key type");
    }
  }
}

export function deepClone<T>(original: T): T {
  return JSON.parse(JSON.stringify(original));
}

export function filterObject(
  object: Record<string, unknown>,
  filter: (key: string) => boolean
): Record<string, unknown> {
  return Object.keys(object).reduce(
    (accum: Record<string, unknown>, key: string) => {
      if (filter(key)) {
        accum[key] = object[key];
      }
      return accum;
    },
    {}
  );
}

export function flipSign(value: string | null): string {
  switch (value) {
    case "-":
      return "";
    case null:
      return "-";
    default:
      if (value.charAt(0) === "-") {
        return value.substring(1);
      } else {
        return "-" + value;
      }
  }
}

export function applyJsTemplate(
  object: Record<string, unknown>,
  labelTemplate: string
): string {
  if (object === undefined || object === null) {
    return "";
  }
  const ids = Object.keys(object);
  const vals = Object.values(object);
  try {
    const label = new Function(...ids, "return `" + labelTemplate + "`;")(
      ...vals
    );
    if (label) {
      return label;
    }
  } catch (err: unknown) {
    // The template is not working, lets try and fail gracefully
    const message = err instanceof Error ? err.message : String(err);
    console.log(
      message +
        " we got keys:" +
        JSON.stringify(ids) +
        " vals:" +
        JSON.stringify(vals) +
        " and template: " +
        labelTemplate
    );
  }
  if (object.hasOwnProperty("primaryKey")) {
    return flattenObject(object.primaryKey as Record<string, unknown>);
  }

  if (object.hasOwnProperty("name")) {
    return String(object.name);
  }

  if (object.hasOwnProperty("id")) {
    return String(object.id);
  }
  return flattenObject(object);
}

/** horrible that this is not standard, found this here https://dmitripavlutin.com/how-to-compare-objects-in-javascript/#4-deep-equality*/
export function deepEqual(
  object1: Record<string, unknown>,
  object2: Record<string, unknown>
): boolean {
  const keys1 = Object.keys(object1);
  const keys2 = Object.keys(object2);
  if (keys1.length !== keys2.length) {
    return false;
  }
  for (const key of keys1) {
    const val1 = object1[key];
    const val2 = object2[key];
    const areObjects = isObject(val1) && isObject(val2);
    if (
      (areObjects &&
        !deepEqual(
          val1 as Record<string, unknown>,
          val2 as Record<string, unknown>
        )) ||
      (!areObjects && val1 !== val2)
    ) {
      return false;
    }
  }
  return true;
}

function isObject(object: unknown): object is Record<string, unknown> {
  return object !== null && typeof object === "object";
}

export function applyComputed(rows: IRow[], tableMetadata: ITableMetaData) {
  return rows?.map((row) => {
    return tableMetadata.columns.reduce((accum: IRow, column: IColumn) => {
      if (column.computed && column.columnType !== AUTO_ID) {
        try {
          accum[column.id] = executeExpression(
            column.computed,
            row,
            tableMetadata
          );
        } catch (error) {
          console.log("Computed expression failed: ", error);
          accum[column.id] = "error: could not compute value: " + error;
        }
      } else if (row.hasOwnProperty(column.id)) {
        accum[column.id] = row[column.id];
      } else {
        // don't add empty property that didn't exist before
      }
      return accum;
    }, {});
  });
}
