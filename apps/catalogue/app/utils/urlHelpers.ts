import type { KeyObject } from "../../../metadata-utils/src/types";

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

/**
 * Builds a canonical URL for SEO purposes by normalizing the catalogue path
 * to `/all/` and stripping filter/view query parameters while preserving pagination.
 * This prevents duplicate content issues when the same resource is accessible
 * under multiple catalogue paths (e.g. `/NCC/collections/X` and `/all/collections/X`).
 */
export function buildCanonicalUrl(
  requestUrl: URL,
  routeParams: { catalogue?: string }
): string {
  const url = new URL(requestUrl);
  if (routeParams.catalogue && routeParams.catalogue !== "all") {
    url.pathname = url.pathname.replace(
      new RegExp(`^/${routeParams.catalogue}/`),
      "/all/"
    );
  }
  // Keep pagination param, strip filter/view params to avoid duplicate content
  const page = url.searchParams.get("page");
  const canonicalParams = page ? `?page=${page}` : "";
  return url.origin + url.pathname + canonicalParams;
}
