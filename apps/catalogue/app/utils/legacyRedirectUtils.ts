import { RESERVED_ROUTES } from "./constants";

/**
 * Given a path, returns the redirect target for legacy 3-segment catalogue routes,
 * or null if no redirect is needed.
 */
export function getLegacyRedirectTarget(path: string): string | null {
  const pathSegments = path.split("/").filter(Boolean);

  if (pathSegments.length !== 3) {
    return null;
  }

  const first = pathSegments[0] as string;
  const resourceType = pathSegments[1] as string;
  const resourceId = pathSegments[2] as string;

  if (RESERVED_ROUTES.includes(first)) {
    return null;
  }

  if (resourceType === "collections" || resourceType === "networks") {
    return `/${resourceId}?catalogue=${first}`;
  }

  return null;
}
