import { joinURL } from "ufo";
import { createConsola } from "consola";
import { defineEventHandler, readBody, getRouterParam, proxyRequest } from "h3";
import { useRuntimeConfig } from "#imports";

export default defineEventHandler((event) => {
  const config = useRuntimeConfig(event);
  const logger = createConsola({ level: (config.logLevel as number) ?? 3 });
  logger.info("proxy schema gql request : ", event.path);
  if (event.method === "POST") {
    readBody(event).then((body) => {
      if (body.query) {
        logger.debug(body.query);
      }
      if (body.variables) {
        logger.debug(body.variables);
      }
    });
  }
  const schema = getRouterParam(event, "schema") || "";
  logger.info("to : ", joinURL(config.public.apiBase, schema, "graphql"));
  const target = joinURL(config.public.apiBase, schema, "graphql");
  return proxyRequest(event, target);
});
