import { defineNuxtRouteMiddleware, navigateTo } from "#app";
import { parseLinkSetting } from "../../../tailwind-components/app/utils/typeUtils";
import { useSchemaSettings } from "../../../tailwind-components/app/composables/useSchemaSettings";
const SCHEMA_LANDING_PAGE = "SCHEMA_LANDING_PAGE";

// Navigate to location specified in schema settings.SCHEMA_LANDING_PAGE if it exists
export default defineNuxtRouteMiddleware(async (to, from) => {
  console.log("Running landing page middleware");
  console.log("From:", from);
  console.log("To:", to);

  const setting = await useSchemaSettings(new Set([SCHEMA_LANDING_PAGE]), to);

  if (from.path === "/" && setting?.SCHEMA_LANDING_PAGE) {
    const landingPageLink = parseLinkSetting(setting.SCHEMA_LANDING_PAGE);
    return await navigateTo(landingPageLink.link, {
      replace: true,
      external: landingPageLink.isSpaLink,
    });
  }
});
