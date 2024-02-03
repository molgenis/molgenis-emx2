import { joinURL } from "ufo";
export default defineEventHandler((event) => {
  const config = useRuntimeConfig(event);
  console.log("proxy message request : ", event.path);
  const schema = getRouterParam(event, "schema") || "";
  const target = joinURL(config.public.proxyTarget, schema, "message");
  return proxyRequest(event, target);
});
