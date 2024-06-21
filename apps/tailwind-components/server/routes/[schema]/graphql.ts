import { joinURL } from "ufo";
export default defineEventHandler((event) => {
 const config = useRuntimeConfig(event); 
  console.log("proxy schema gql request : ", event.path);
  const schema = getRouterParam(event, "schema") || "";
  console.log("to : ", joinURL(config.public.apiBase, schema, "graphql"));
  const target = joinURL(config.public.apiBase, schema,"graphql");
  return proxyRequest(event, target);
});
