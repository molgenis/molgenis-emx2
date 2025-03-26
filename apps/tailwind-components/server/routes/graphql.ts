import { joinURL } from "ufo";
export default defineEventHandler((event) => {
  const config = useRuntimeConfig(event);
  console.log("proxy gql request : ", event.path);
  const schema = getRouterParam(event, "schema") || "";
  console.log("to : ", joinURL(config.public.apiBase, "graphql"));
  const target = joinURL(config.public.apiBase, "api/graphql");
  return proxyRequest(event, target);
});
