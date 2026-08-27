import { defineNuxtRouteMiddleware, navigateTo } from "#app";
import { parseLinkSetting } from "../../../tailwind-components/app/utils/typeUtils";
import { useSchemaSettings } from "../../../tailwind-components/app/composables/useSchemaSettings";
const SCHEMA_LANDING_PAGE = "SCHEMA_LANDING_PAGE";

export default defineNuxtRouteMiddleware(async (to, from) => {
  const setting = await useSchemaSettings(new Set([SCHEMA_LANDING_PAGE]), to);
  const schemaLandingPage = setting?.value?.SCHEMA_LANDING_PAGE;

  if (from.path === "/" && typeof schemaLandingPage === "string") {
    const landingPageLink = parseLinkSetting(schemaLandingPage);
    return await navigateTo(landingPageLink.link, {
      replace: true,
      external: !landingPageLink.isSpaLink,
    });
  }
});
