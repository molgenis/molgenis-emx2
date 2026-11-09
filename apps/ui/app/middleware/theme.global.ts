import { defineNuxtRouteMiddleware } from "#app";
import { useThemeSettings } from "../composables/useThemeSettings";

export default defineNuxtRouteMiddleware(async (to) => {
  const schema = to.params.schema as string | undefined;
  const { load, applySavedTheme } = useThemeSettings(schema);
  await load();
  applySavedTheme(to.query.theme);
});
