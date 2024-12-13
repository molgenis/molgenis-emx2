import { joinURL } from "ufo";
export default defineEventHandler((event) => {
 const config = useRuntimeConfig(event); 
  console.log("proxy single trigger request : ", event.path, ' type: ', event.method);
  const location = joinURL(config.public.apiBase, event.path)
  console.log("to : ", location);
  return proxyRequest(event, location);
});