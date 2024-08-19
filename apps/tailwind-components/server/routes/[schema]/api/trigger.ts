import { joinURL } from "ufo";
export default defineEventHandler((event) => {
 const config = useRuntimeConfig(event); 
  console.log("proxy trigger request : ", event.path);
  const schema = getRouterParam(event, "schema") || "";
  const location = joinURL(config.public.apiBase, schema, "api", "trigger")
  console.log("to : ", location);
  return proxyRequest(event, location);
});
