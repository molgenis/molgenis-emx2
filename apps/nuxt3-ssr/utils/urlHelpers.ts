import type{ KeyObject } from "meta-data-utils";

export const transformToKeyObject = (keyObjectAsString: string): KeyObject =>
  JSON.parse(keyObjectAsString);

/**
 * Generates human readable key from KeyObject, one way only, only used for readability
 */
export const buildValueKey = (keyObject: KeyObject): string => {
  return Object.values(keyObject).reduce(
    (acc: string, val: string | KeyObject) => {
      const joiner = acc.length === 0 ? "" : "-";
      return (acc +=
        joiner + (typeof val === "string" ? val : buildValueKey(val)));
    },
    ""
  );
};

export const resourceIdPath = (keyObject: KeyObject) => {
  return buildValueKey(keyObject) + "?keys=" + JSON.stringify(keyObject);
};
