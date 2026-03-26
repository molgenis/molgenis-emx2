import type {
  columnValue,
  columnValueObject,
} from "../../../metadata-utils/src/types";
import { flattenObject } from "./flattenObject";

export function columnValueToString(
  value: columnValue,
  labelTemplate: string
): string | undefined {
  if (value === undefined || value === null) {
    return "";
  }
  const ids = Object.keys(value);
  const vals = Object.values(value);
  try {
    const label = new Function(...ids, "return `" + labelTemplate + "`;")(
      ...vals
    );
    if (label) {
      return label;
    }
  } catch (err: any) {
    console.log(
      `${err.message} we got keys: ${JSON.stringify(
        ids
      )} vals: ${JSON.stringify(vals)} and template: ${labelTemplate}`
    );
  }
  assertColumnValueObject(value);
  if (value.hasOwnProperty("primaryKey")) {
    assertPrimaryKey(value);
    return flattenObject(value["primaryKey"]);
  }

  if (value.hasOwnProperty("name")) {
    assertName(value);
    return value.name !== null ? String(value.name) : undefined;
  }

  if (value.hasOwnProperty("id")) {
    assertId(value);
    return value.id !== null ? String(value.id) : undefined;
  }
  return flattenObject(value);
}

function assertColumnValueObject(
  column: columnValue
): asserts column is columnValueObject {
  if (typeof column !== "object" || column === null) {
    throw new Error("Value is not a valid column value");
  }
}

function assertPrimaryKey(
  column: columnValueObject
): asserts column is columnValueObject & { primaryKey: any } {
  if (!("primaryKey" in column)) {
    throw new Error("Value does not have a primaryKey property");
  }
}

function assertName(
  column: columnValueObject
): asserts column is columnValueObject & { name: any } {
  if (!("name" in column)) {
    throw new Error("Value does not have a name property");
  }
}

function assertId(
  column: columnValueObject
): asserts column is columnValueObject & { id: any } {
  if (!("id" in column)) {
    throw new Error("Value does not have an id property");
  }
}
