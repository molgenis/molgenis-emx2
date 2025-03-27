import { joinURL } from "ufo";
import { proxyRequest, defineEventHandler } from "h3";
import { useRuntimeConfig } from "#imports";
export default defineEventHandler((event) => {
 const config = useRuntimeConfig(event); 
  console.log("proxy single trigger request : ", event.path, ' type: ', event.method);
  const location = joinURL(config.public.apiBase, event.path)
  console.log("to : ", location);
  return proxyRequest(event, location);
});