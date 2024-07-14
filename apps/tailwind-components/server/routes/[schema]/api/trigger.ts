import { joinURL } from "ufo";
export default defineEventHandler((event) => {
 const config = useRuntimeConfig(event); 
  console.log("proxy trigger request : ", event.path);
  const schema = getRouterParam(event, "schema") || "";
  const triggerPreviewLocation = "https://preview-emx2-pr-3946.dev.molgenis.org/"
  //const location = joinURL(config.public.apiBase, schema, "api", "trigger")
  const location = joinURL(triggerPreviewLocation, schema, "api", "trigger")
  console.log("to : ", location);
  return proxyRequest(event, location);
});
