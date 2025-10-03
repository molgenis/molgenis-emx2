import { joinURL } from "ufo"; 
import { proxyRequest, defineEventHandler } from "h3";
import { useRuntimeConfig } from "#imports";
export default defineEventHandler((event) => {
  const config = useRuntimeConfig(event);
  // console.log("proxy file request : ", event.path);
  const target = joinURL(config.public.apiBase, event.path);
  return proxyRequest(event, target);
});
