import { defineNuxtRouteMiddleware, navigateTo } from "#app";
import { useSchemaSettings } from "../../../tailwind-components/app/composables/useSchemaSettings";
const SCHEMA_LANDING_PAGE = "SCHEMA_LANDING_PAGE";

// Navigate to location specified in schema settings.LANDING_PAGE if it exists
export default defineNuxtRouteMiddleware(async (to) => {
  const setting = await useSchemaSettings(new Set([SCHEMA_LANDING_PAGE]), to);
  console.log("Landing page middleware:", setting?.SCHEMA_LANDING_PAGE);

  if (
    typeof setting?.SCHEMA_LANDING_PAGE === "string" &&
    setting.SCHEMA_LANDING_PAGE !== ""
  ) {
    return navigateTo(setting.SCHEMA_LANDING_PAGE, {
      replace: true,
    });
  }
});
