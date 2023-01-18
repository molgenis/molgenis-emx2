import { IColumn } from "../Interfaces/IColumn";
import { IRow } from "../Interfaces/IRow";
import { ITableMetaData } from "../Interfaces/ITableMetaData";
import constants from "./constants";

const { CODE_0, CODE_9, CODE_BACKSPACE, CODE_DELETE, MIN_LONG, MAX_LONG } =
  constants;

export function isNumericKey(event: KeyboardEvent): boolean {
  const keyCode = event.which ?? event.keyCode;
  return (
    (keyCode >= CODE_0 && keyCode <= CODE_9) ||
    keyCode === CODE_BACKSPACE ||
    keyCode === CODE_DELETE
  );
}

export function flattenObject(object: Record<string, any>) {
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

export function getPrimaryKey(row: IRow, tableMetaData: ITableMetaData) {
  //we only have pkey when the record has been saved
  if (!row["mg_insertedOn"] || !tableMetaData) {
    return null;
  } else {
    return tableMetaData.columns?.reduce(
      (accum: Record<string, any>, column: IColumn) => {
        if (column.key === 1 && row[column.id]) {
          accum[column.id] = row[column.id];
        }
        return accum;
      },
      {}
    );
  }
}

export function deepClone(original: any): any {
  // node js may not have structuredClone function, then fallback to deep clone via JSON
  // return typeof structuredClone === "function"
  //   ? structuredClone(original)
  //   :
  //structuredClone doesn't work in vue 3
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
  if (value === "-" || isInvalidBigInt(value)) {
    return BIG_INT_ERROR;
  } else {
    return undefined;
  }
}

export function isInvalidBigInt(value: string): boolean {
  return (
    value !== null &&
    (BigInt(value) > BigInt(MAX_LONG) || BigInt(value) < BigInt(MIN_LONG))
  );
}

export function convertToCamelCase(string: string): string {
  const words = string.trim().split(/\s+/);
  let result = "";
  words.forEach((word: string, index: number) => {
    if (index === 0) {
      result += word.charAt(0).toLowerCase();
    } else {
      result += word.charAt(0).toUpperCase();
    }
    if (word.length > 1) {
      result += word.slice(1);
    }
  });
  return result;
}

export function convertToPascalCase(string: string): string {
  const words = string.trim().split(/\s+/);
  let result = "";
  words.forEach((word: string) => {
    result += word.charAt(0).toUpperCase();
    if (word.length > 1) {
      result += word.slice(1);
    }
  });
  return result;
}
