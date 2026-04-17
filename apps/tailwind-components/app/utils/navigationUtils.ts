import type { nonEmptyColumnValue } from "../../../metadata-utils/src/types";

/**
 * Generates human readable key from KeyObject, one way only, only used for readability
 */
export function keySlug(
  keyObject: Record<string, nonEmptyColumnValue> | nonEmptyColumnValue
): string {
  return Object.values(keyObject).reduce((acc: string, val) => {
    const joiner = acc.length === 0 ? "" : "-";
    return (acc += joiner + (typeof val === "string" ? val : keySlug(val)));
  }, "");
}
