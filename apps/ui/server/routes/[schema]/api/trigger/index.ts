import { joinURL } from "ufo";
import { proxyRequest, defineEventHandler, readBody, getRouterParam } from "h3";
import { useRuntimeConfig } from "#imports";
export default defineEventHandler((event) => {
 const config = useRuntimeConfig(event); 
  console.log("proxy trigger request : ", event.path, ' type: ', event.method);
  const schema = getRouterParam(event, "schema") || "";
  const location = joinURL(config.public.apiBase, schema, "api", "trigger")
  console.log("to : ", location);
  return proxyRequest(event, location);
});