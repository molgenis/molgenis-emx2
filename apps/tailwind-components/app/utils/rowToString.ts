import type { IRow } from "../../../metadata-utils/src/types";
import { flattenObject } from "./flattenObject";

export function rowToString(
  row: IRow,
  labelTemplate: string
): string | undefined {
  if (row === undefined || row === null) {
    return "";
  }
  const ids = Object.keys(row);
  const vals = Object.values(row);
  try {
    const label = new Function(...ids, "return `" + labelTemplate + "`;")(
      ...vals
    );
    if (label) {
      return label;
    }
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : String(err);
    console.log(
      `${message} we got keys: ${JSON.stringify(ids)} vals: ${JSON.stringify(
        vals
      )} and template: ${labelTemplate}`
    );
  }
  if (row.hasOwnProperty("primaryKey")) {
    return flattenObject(row.primaryKey as Record<string, unknown>);
  }

  if (row.hasOwnProperty("name")) {
    return row.name !== null ? String(row.name) : undefined;
  }

  if (row.hasOwnProperty("id")) {
    return row.id !== null ? String(row.id) : undefined;
  }
  return flattenObject(row);
}
