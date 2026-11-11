import { defineNuxtRouteMiddleware, navigateTo, useAppConfig } from "#app";
import {
  parseLinkSetting,
  resolveLinkLocation,
} from "../../../tailwind-components/app/utils/typeUtils";
import { useSchemaSettings } from "../../../tailwind-components/app/composables/useSchemaSettings";
const SCHEMA_LANDING_PAGE = "SCHEMA_LANDING_PAGE";

export default defineNuxtRouteMiddleware(async (to, from) => {
  const setting = await useSchemaSettings(new Set([SCHEMA_LANDING_PAGE]), to);
  const schemaLandingPage = setting?.value?.SCHEMA_LANDING_PAGE;

  if (from.path === "/" && typeof schemaLandingPage === "string") {
    const landingPageLink = parseLinkSetting(schemaLandingPage);
    const location = resolveLinkLocation(
      landingPageLink,
      String(useAppConfig().basePath ?? "/")
    );
    if (location) {
      return await navigateTo(location, {
        replace: true,
        external: landingPageLink.linkType !== "spaLink",
      });
    }
  }
});
