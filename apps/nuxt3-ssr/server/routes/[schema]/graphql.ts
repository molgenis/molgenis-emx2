import { joinURL } from "ufo";
import { createConsola } from "consola";

export default defineEventHandler((event) => {
  const config = useRuntimeConfig(event);
  const logger = createConsola({ level: config.logLevel as number ?? 5 });
  if (event.method === "POST") {
    readBody(event).then((body) => {
      if (body.query) {
        logger.debug( body.query);
      }
      if (body.variables) {
        logger.debug('Query variables: ', body.variables);
      }
    });
  }
  const schema = getRouterParam(event, "schema") || "";
  const target = joinURL(config.public.apiBase, schema, "graphql");
  return proxyRequest(event, target);
});