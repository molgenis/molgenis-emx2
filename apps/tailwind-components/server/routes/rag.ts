import { useRuntimeConfig } from "#imports";
import { proxyRequest, defineEventHandler } from "h3";
import { joinURL } from "ufo";
export default defineEventHandler((event) => {
  const config = useRuntimeConfig(event);
  console.log("proxy rag request : ", event.path);
  console.log("to : ", joinURL(config.public.apiBase, "rag"));
  const target = joinURL(config.public.apiBase, "api/rag");
  return proxyRequest(event, target);
});
