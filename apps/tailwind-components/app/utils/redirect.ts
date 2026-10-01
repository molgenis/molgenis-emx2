import type { LocationQueryValue, RouteLocationNormalized } from "vue-router";

// fixed placeholder origin: works during SSR (no window) and makes any
// absolute url, including one to our own host, resolve to a different origin
const BASE = "http://redirect.invalid";

/**
 * Validate a `redirectTo` query value before it is used to navigate.
 *
 * `/login?redirectTo=...` is attacker controlled, so the value is only accepted
 * when it is a path on the host we are already on. Anything that could leave
 * this host (an absolute url, a protocol relative `//host` url, a `javascript:`
 * url) or that the router cannot resolve is rejected with an error.
 */
export function sanitizeRedirectPath(
  redirectTo: LocationQueryValue | LocationQueryValue[]
): string {
  const candidate = Array.isArray(redirectTo) ? redirectTo[0] : redirectTo;
  if (typeof candidate !== "string") {
    throw new Error("Invalid redirect: expected a path");
  }

  // the url parser would resolve a relative path against BASE, so only
  // accept values that are already absolute paths
  const trimmed = candidate.trim();
  if (!trimmed.startsWith("/")) {
    throw new Error(
      `Invalid redirect: '${candidate}' is not a path on this host`
    );
  }

  // Use the browser's url parser normalise control characters and
  // backslashes, then check that the result did not leave this host
  const url = new URL(trimmed, BASE);
  if (url.origin !== BASE) {
    throw new Error(
      `Invalid redirect: '${candidate}' is not a path on this host`
    );
  }

  return url.pathname + url.search + url.hash;
}
