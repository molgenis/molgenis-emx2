import { joinURL } from "ufo";
export default defineEventHandler((event) => {
  const config = useRuntimeConfig(event);
  // console.log("proxy gql request : ", event.path);
  const schema = getRouterParam(event, "schema") || "";
  const target = joinURL(config.public.apiBase, schema, "graphql");
  return proxyRequest(event, target);
});
