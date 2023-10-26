import { IColumn } from "../Interfaces/IColumn";
import { IRow } from "../Interfaces/IRow";
import { ITableMetaData } from "../Interfaces/ITableMetaData";
import constants from "./constants";
import Client from "../client/client";

const { CODE_0, CODE_9, CODE_BACKSPACE, CODE_DELETE, MIN_LONG, MAX_LONG } =
  constants;

export function isRefType(columnType: string): boolean {
  return ["REF", "REF_ARRAY", "REFBACK", "ONTOLOGY", "ONTOLOGY_ARRAY"].includes(
    columnType
  );
}

export function isNumericKey(event: KeyboardEvent): boolean {
  const keyCode = event.which ?? event.keyCode;
  return (
    (keyCode >= CODE_0 && keyCode <= CODE_9) ||
    keyCode === CODE_BACKSPACE ||
    keyCode === CODE_DELETE
  );
}

export function flattenObject(object: Record<string, any>): string {
  if (typeof object === "object") {
    let result = "";
    Object.keys(object).forEach((key) => {
      if (object[key] === null) {
        return;
      }
      if (typeof object[key] === "object") {
        result += flattenObject(object[key]);
      } else {
        result += " " + object[key];
      }
    });
    return result;
  } else {
    return object;
  }
}

export async function convertRowToPrimaryKey(
  row: IRow,
  tableId: string,
  schemaId: string
): Promise<Record<string, any>> {
  const client = Client.newClient(schemaId);
  const tableMetadata = await client.fetchTableMetaData(tableId);
  if (!tableMetadata?.columns) {
    throw new Error("Empty columns in metadata");
  } else {
    return await tableMetadata.columns.reduce(
      async (accumPromise: Promise<IRow>, column: IColumn): Promise<IRow> => {
        let accum: IRow = await accumPromise;
        const cellValue = row[column.id];
        if (column.key === 1 && cellValue) {
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

async function getKeyValue(cellValue: any, column: IColumn, schemaId: string) {
  if (typeof cellValue === "string") {
    return cellValue;
  } else {
    if (column.refTableId) {
      return await convertRowToPrimaryKey(
        cellValue,
        column.refTableId,
        schemaId
      );
    }
  }
}

export function deepClone(original: any): any {
  return JSON.parse(JSON.stringify(original));
}

export function filterObject(
  object: Record<string, any>,
  filter: (key: string) => boolean
): Record<string, any> {
  return Object.keys(object).reduce(
    (accum: Record<string, any>, key: string) => {
      if (filter(key)) {
        accum[key] = object[key];
      }
      return accum;
    },
    {}
  );
}

export function flipSign(value: string): string | null {
  switch (value) {
    case "-":
      return null;
    case null:
      return "-";
    default:
      if (value.toString().charAt(0) === "-") {
        return value.toString().substring(1);
      } else {
        return "-" + value;
      }
  }
}

const BIG_INT_ERROR = `Invalid value: must be value from ${MIN_LONG} to ${MAX_LONG}`;

export function getBigIntError(value: string): string | undefined {
  if (isInvalidBigInt(value)) {
    return BIG_INT_ERROR;
  } else {
    return undefined;
  }
}

export function isInvalidBigInt(value: string): boolean {
  const isValidRegex = /^-?\d+$/;
  if (Boolean(value) && isValidRegex.test(value)) {
    return BigInt(value) > BigInt(MAX_LONG) || BigInt(value) < BigInt(MIN_LONG);
  } else {
    return true;
  }
}

export function applyJsTemplate(
  object: object,
  labelTemplate: string
): string | undefined {
  if (object === undefined || object === null) {
    return "";
  }
  const ids = Object.keys(object);
  const vals = Object.values(object);
  try {
    // @ts-ignore
    return new Function(...ids, "return `" + labelTemplate + "`;")(...vals);
  } catch (err: any) {
    // The template is not working, lets try and fail gracefully
    console.log(
      err.message +
        " we got keys:" +
        JSON.stringify(ids) +
        " vals:" +
        JSON.stringify(vals) +
        " and template: " +
        labelTemplate
    );

    if (object.hasOwnProperty("primaryKey")) {
      return flattenObject((object as any).primaryKey);
    }

    if (object.hasOwnProperty("name")) {
      return (object as any).name;
    }

    if (object.hasOwnProperty("id")) {
      return (object as any).id;
    }
    return flattenObject(object);
  }
}

/** horrible that this is not standard, found this here https://dmitripavlutin.com/how-to-compare-objects-in-javascript/#4-deep-equality*/
export function deepEqual(
  object1: Record<string, any>,
  object2: Record<string, any>
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
      (areObjects && !deepEqual(val1, val2)) ||
      (!areObjects && val1 !== val2)
    ) {
      return false;
    }
  }
  return true;
}

function isObject(object: Record<string, any>): object is Object {
  return object !== null && typeof object === "object";
}
