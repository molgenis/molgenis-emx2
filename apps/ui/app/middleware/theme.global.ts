import { defineNuxtRouteMiddleware } from "#app";
import { resolveUiTheme } from "../../../tailwind-components/app/composables/useUiTheme";

// Set the theme from the UI_THEME (or SCHEMA_UI_THEME on schema routes) setting before the page renders
export default defineNuxtRouteMiddleware(async (to) => {
  await resolveUiTheme(to);
});
