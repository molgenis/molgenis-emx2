import { defineEventHandler, type H3Event } from "h3";

const PAGE_METHODS = new Set(["GET", "HEAD"]);

export function dropPageOriginHeaders(event: H3Event) {
  if (!PAGE_METHODS.has(event.method)) return;
  delete event.node.req.headers.origin;
  delete event.node.req.headers.referer;
}

export default defineEventHandler(dropPageOriginHeaders);
