export type KeyObject = {
  [key: string]: KeyObject | string;
};

/**
 * Generates human readable key from KeyObject, one way only, only used for readability
 */
export function keySlug(keyObject: KeyObject): string {
  return Object.values(keyObject).reduce(
    (acc: string, val: string | KeyObject) => {
      const joiner = acc.length === 0 ? "" : "-";
      return (acc += joiner + (typeof val === "string" ? val : keySlug(val)));
    },
    ""
  );
}
