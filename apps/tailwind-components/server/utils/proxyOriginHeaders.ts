import { getRequestHeaders, type H3Event } from "h3";

// The backend rejects state-changing requests whose Origin is another host (CSRF protection).
// Re-label requests made by this dev server's own pages with the origin of the proxy target;
// requests from other sites keep their Origin and are still rejected. Mirrors apps/dev-proxy-origin.js
export function proxyOriginHeaders(
  event: H3Event,
  target: string
): Record<string, string> {
  const headers = getRequestHeaders(event);
  const source = headers.origin || headers.referer;
  if (!source || !headers.host) return {};
  try {
    if (new URL(source).host !== headers.host) return {};
    return { origin: new URL(target).origin };
  } catch {
    return {};
  }
}
