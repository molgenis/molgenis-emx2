import { joinURL } from "ufo";
import { proxyRequest, defineEventHandler, readBody, getRouterParam } from "h3";
import { useRuntimeConfig } from "#imports";
export default defineEventHandler((event) => {
  const config = useRuntimeConfig(event);
  console.log("proxy trigger request : ", event.path);
  const schema = getRouterParam(event, "schema") || "";
  // const target = joinURL(config.public.apiBase, schema, "trigger");
  const target = joinURL('http://localhost:8080/', schema, 'api', "trigger");
  return proxyRequest(event, target);
});
