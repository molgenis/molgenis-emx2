import constants from "./constants";
import type { IColumn, ITableMetaData, columnValueObject} from "../../metadata-utils/src/types";
import {executeExpression} from "~/utils/formUtils";

const { CODE_0, CODE_9, CODE_PERIOD, MIN_LONG, MAX_LONG, AUTO_ID } = constants;

export function isRefType(columnType: string): boolean {
  return ["REF", "REF_ARRAY", "REFBACK", "ONTOLOGY", "ONTOLOGY_ARRAY"].includes(
    columnType
  );
}

export function isNumericKey(event: KeyboardEvent): boolean {
  const keyCode = event.which ?? event.keyCode;
  return (keyCode >= CODE_0 && keyCode <= CODE_9) || keyCode === CODE_PERIOD;
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

// export async function convertRowToPrimaryKey(
//   row: columnValueObject,
//   tableId: string,
//   schemaId: string
// ): Promise<Record<string, any>> {
//   const client = Client.newClient(schemaId);
//   const tableMetadata = await client.fetchTableMetaData(tableId);
//   if (!tableMetadata?.columns) {
//     throw new Error("Empty columns in metadata");
//   } else {
//     return await tableMetadata.columns.reduce(
//       async (accumPromise: Promise<IRow>, column: IColumn): Promise<IRow> => {
//         let accum: IRow = await accumPromise;
//         const cellValue = row[column.id];
//         if (column.key === 1 && cellValue) {
//           accum[column.id] = await getKeyValue(
//             cellValue,
//             column,
//             column.refSchemaId || schemaId
//           );
//         }
//         return accum;
//       },
//       Promise.resolve({})
//     );
//   }
// }
//
// async function getKeyValue(cellValue: any, column: IColumn, schemaId: string) {
//   if (typeof cellValue === "string") {
//     return cellValue;
//   } else {
//     if (column.refTableId) {
//       return await convertRowToPrimaryKey(
//         cellValue,
//         column.refTableId,
//         schemaId
//       );
//     }
//   }
// }

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

export function flipSign(value: string | null): string | null {
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
  object: Record<string, any>,
  labelTemplate: string
): string | undefined {
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
  }
  if (object.hasOwnProperty("primaryKey")) {
    return flattenObject(object.primaryKey);
  }

  if (object.hasOwnProperty("name")) {
    return object.name;
  }

  if (object.hasOwnProperty("id")) {
    return object.id;
  }
  return flattenObject(object);
}

/** horrible that this is not standard, found this here https://dmitripavlutin.com/how-to-compare-objects-in-javascript/#4-deep-equality*/
export function deepEqual(
  object1: Record<string, any>,
  object2: Record<stroing, any>
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

function isObject(object: Record<string, any> | null): object is Object {
  return object !== null && typeof object === "object";
}

export function applyComputed(rows: columnValueObject[], schemaId: string, tableMetadata: ITableMetaData) {
  return rows?.map((row) => {
    return tableMetadata.columns.reduce((accum: columnValueObject, column: IColumn) => {
      if (column.computed && column.columnType !== AUTO_ID) {
        try {
          accum[column.id] = executeExpression(
            column.computed,
            row,
            schemaId,
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
