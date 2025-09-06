import { joinURL } from "ufo";
import { createConsola } from "consola";
import { defineEventHandler, readBody, getRouterParam, proxyRequest } from "h3";
import { useRuntimeConfig } from "#imports";

export default defineEventHandler((event) => {
  const config = useRuntimeConfig(event);
  const logger = createConsola({ level: (config.logLevel as number) ?? 3 });
  logger.info("proxy schema rdf request : ", event.path);
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
  const host: string = process.env.MOLGENIS_RDF_HOST || config.public.apiBase;
  const schema = getRouterParam(event, "schema") || "";
  const path: string[] = (event._path as string).split("/");
  let target: string;

  // redirect to: host/api/rdf or host/schema/api/rdf(.*)?
  if (event._path?.endsWith("?shacls")) {
    target = joinURL(host, "api", path[path.length - 1]);
  } else if (schema) {
    target = joinURL(host, schema, "api", path[path.length - 1]);
  } else {
    target = joinURL(host, "api", path[path.length - 1]);
  }

  logger.info("to : ", target);

  return proxyRequest(event, target);
});
