import { joinURL } from "ufo";
import { useLogger } from '@nuxt/kit'
import { useAppConfig } from "nuxt/app";

export default defineEventHandler((event) => {
 const config = useRuntimeConfig(event); 
const cong = useAppConfig();

  const logger = useLogger('proxy',  { level: config.logLevel === 'silent' ? 0 : 3 });
  logger.info("proxy schema gql request : ", event.path);
  if (event.method === "POST") {
    readBody(event).then((body) => {
      if (body.query) {
        logger.debug( body.query);
      }
    });
  }
  const schema = getRouterParam(event, "schema") || "";
  logger.info("to : ", joinURL(config.public.apiBase, schema, "graphql"));
  const target = joinURL(config.public.apiBase, schema,"graphql");
  return proxyRequest(event, target);
});
