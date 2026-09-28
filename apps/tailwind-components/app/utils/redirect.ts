/**
 * Validate a `redirectTo` query value before it is used to navigate.
 *
 * `/login?redirectTo=...` is attacker controlled, so the value is only accepted
 * when it is a path on the host we are already on. Anything that could leave
 * this host (an absolute url, a protocol relative `//host` url, a `javascript:`
 * url) or that the router cannot resolve is rejected with an error.
 */
export function sanitizeRedirectPath(redirectTo: unknown): string {
  const candidate = Array.isArray(redirectTo) ? redirectTo[0] : redirectTo;
  if (typeof candidate !== "string") {
    throw new Error("Invalid redirect: expected a path");
  }

  // browsers ignore control characters in urls and read a backslash as a slash,
  // so normalise both before deciding whether this is a plain path
  const target = candidate
    .replace(/[\u0000-\u001F\u007F]/g, "")
    .replaceAll("\\", "/")
    .trim();

  if (!target.startsWith("/") || target.startsWith("//")) {
    throw new Error(
      `Invalid redirect: '${candidate}' is not a path on this host`
    );
  }

  return target;
}
