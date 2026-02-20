import type { Crumb } from "../../../tailwind-components/types/types";

/**
 * Determine the catalogue ID from route information.
 * Returns the query param if present, the resourceId if the path has a sub-route,
 * or null if unscoped.
 */
export function getCatalogueId(
  queryParam: string | undefined,
  resourceId: string | undefined,
  path: string
): string | null {
  if (typeof queryParam === "string") return queryParam;

  if (!resourceId) return null;

  const pathAfterResource = path.slice(`/${resourceId}`.length);
  if (pathAfterResource.length > 1) {
    return resourceId;
  }

  return null;
}

/**
 * Build a URL that preserves the catalogue context via query parameter.
 */
export function buildResourceUrl(
  path: string,
  catalogueId: string | null
): string {
  const base = path.startsWith("/") ? path : `/${path}`;
  if (!catalogueId) {
    return base;
  }
  const pathResourceId = base.split("/")[1]?.split("?")[0];
  if (pathResourceId === catalogueId) {
    return base;
  }
  const separator = base.includes("?") ? "&" : "?";
  return `${base}${separator}catalogue=${catalogueId}`;
}

/**
 * Prepend a catalogue breadcrumb when in a scoped context.
 */
export function buildCatalogueBreadcrumbs(
  items: Crumb[],
  catalogueId: string | null
): Crumb[] {
  if (!catalogueId) {
    return items;
  }

  return [
    {
      label: catalogueId,
      url: buildResourceUrl(catalogueId, catalogueId),
    },
    ...items,
  ];
}
