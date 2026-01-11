import { joinURL } from "ufo";
import { createConsola } from "consola";
import { defineEventHandler, readBody, getRouterParam, proxyRequest } from "h3";
import { useRuntimeConfig } from "#imports";

export default defineEventHandler((event) => {
  const config = useRuntimeConfig(event);
  const logger = createConsola({ level: (config.logLevel as number) ?? 3 });
  logger.info("proxy schema api request : ", event.path);
  if (event.method === "GET") {
    readBody(event).then((body) => {
      if (body.query) {
        logger.debug(body.query);
      }
      if (body.variables) {
        logger.debug(body.variables);
      }
    });
  }
  const host: string = config.public.apiBase;
  const schema = getRouterParam(event, "schema") || "";
  const path: string[] = (event._path as string).split("/");
  let target: string;

  target = joinURL(host, schema, "api", path[path.length - 1] || "");

  logger.info("to : ", target);

  return proxyRequest(event, target);
});
