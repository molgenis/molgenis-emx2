import { useRuntimeConfig } from "#imports";
import { proxyRequest, defineEventHandler } from "h3";
import { joinURL } from "ufo";

export default defineEventHandler((event) => {
  const config = useRuntimeConfig(event);
  const target = joinURL(config.public.apiBase, event.path);
  return proxyRequest(event, target);
});
