import { useRuntimeConfig } from "#imports";
import { proxyRequest, defineEventHandler } from "h3";
import { joinURL } from "ufo";
export default defineEventHandler((event) => {
  const config = useRuntimeConfig(event);
  // console.log("proxy gql request : ", event.path);
  // console.log("to : ", joinURL(config.public.apiBase, "graphql"));
  console.log("CENTRAL proxy gql from main graphql : ", event.path);
  const target = joinURL(config.public.apiBase, "api/graphql");
  return proxyRequest(event, target, {headers: { "MG-Schema": "catalogue-demo" }});
});
