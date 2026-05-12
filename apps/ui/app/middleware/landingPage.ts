import { defineNuxtRouteMiddleware, navigateTo } from "#app";
import { useSchemaSettings } from "../../../tailwind-components/app/composables/useSchemaSettings";
const SCHEMA_LANDING_PAGE = "SCHEMA_LANDING_PAGE";

// Navigate to location specified in schema settings.SCHEMA_LANDING_PAGE if it exists
export default defineNuxtRouteMiddleware(async (to, from) => {
  console.log("Running landing page middleware");
  console.log("From:", from);
  console.log("To:", to);

  const setting = await useSchemaSettings(new Set([SCHEMA_LANDING_PAGE]), to);

  if (
    from.path === "/" &&
    typeof setting?.SCHEMA_LANDING_PAGE === "string" &&
    setting.SCHEMA_LANDING_PAGE !== ""
  ) {
    return navigateTo(setting.SCHEMA_LANDING_PAGE, {
      replace: true,
    });
  }
});
