import { joinURL } from "ufo"; 

export default defineEventHandler((event) => {
  const config = useRuntimeConfig(event);
  console.log("proxy file request : ", event.path);
  const target = joinURL(config.public.proxyTarget, event.path);
  return proxyRequest(event, target);
});
