import { joinURL } from "ufo";
import { proxyRequest, defineEventHandler, getRouterParam } from "h3";
import { useRuntimeConfig } from "#imports";
export default defineEventHandler((event) => {
  const config = useRuntimeConfig(event);
  console.log("proxy message request : ", event.path);
  const schema = getRouterParam(event, "schema") || "";
  const target = joinURL(config.public.apiBase, schema, "message");
  return proxyRequest(event, target);
});
