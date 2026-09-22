import { joinURL } from "ufo";
import { defineEventHandler, sendRedirect } from "h3";
import { useRuntimeConfig } from "#imports";
export default defineEventHandler((event) => {
  const config = useRuntimeConfig(event);
  console.log(
    "proxy oidc login request : ",
    event.path,
    " type: ",
    event.method
  );
  const location = joinURL(
    config.public.apiBase,
    event.path.replace("/oidc-login", "/_login")
  );
  console.log("to : ", location);
  return sendRedirect(event, location, 302);
});
