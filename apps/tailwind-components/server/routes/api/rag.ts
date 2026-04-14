import { getRequestURL, useRuntimeConfig } from "#imports";
import { proxyRequest, defineEventHandler } from "h3";
import { joinURL } from "ufo";
export default defineEventHandler((event) => {
  const config = useRuntimeConfig(event);

  const url = getRequestURL(event);

  const base = joinURL(config.public.ragApiBase, "api/rag");

  const target = base + url.search;

  console.log("proxy rag request:", event.path);
  console.log("to:", target);

  return proxyRequest(event, target);
});
