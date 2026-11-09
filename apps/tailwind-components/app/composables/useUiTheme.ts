import { useState } from "#app/composables/state";
import type { RouteLocationNormalizedGeneric } from "vue-router";
import { useSettings } from "./useSettings";
import { useSchemaSettings } from "./useSchemaSettings";

const UI_THEME = "UI_THEME";
const SCHEMA_UI_THEME = "SCHEMA_UI_THEME";
const SYSTEM_THEME_CACHE_KEY = "";

export const TOGGLEABLE_THEMES = ["light", "dark"];
export const useActiveUiTheme = () => useState<string>("uiTheme", () => "");

// per app session cache of fetched themes, keyed by schema id ("" for system)
const useUiThemeCache = () =>
  useState<Record<string, string>>("uiThemeCache", () => ({}));

async function fetchSystemTheme(): Promise<string> {
  const cache = useUiThemeCache();
  if (!(SYSTEM_THEME_CACHE_KEY in cache.value)) {
    try {
      const settings = await useSettings(new Set([UI_THEME]));
      cache.value[SYSTEM_THEME_CACHE_KEY] =
        (settings.value?.[UI_THEME] as string) || "";
    } catch (error) {
      console.error("Error fetching UI_THEME setting", error);
      cache.value[SYSTEM_THEME_CACHE_KEY] = "";
    }
  }
  return cache.value[SYSTEM_THEME_CACHE_KEY] as string;
}

async function fetchSchemaTheme(
  schema: string,
  route: RouteLocationNormalizedGeneric
): Promise<string> {
  const cache = useUiThemeCache();
  if (!Object.prototype.hasOwnProperty.call(cache.value, schema)) {
    try {
      const settings = await useSchemaSettings(
        new Set([SCHEMA_UI_THEME]),
        route
      );
      cache.value[schema] =
        (settings?.value?.[SCHEMA_UI_THEME] as string) || "";
    } catch (error) {
      console.error("Error fetching SCHEMA_UI_THEME setting", error);
      return "";
    }
  }
  return cache.value[schema] as string;
}

export async function resolveUiTheme(route: RouteLocationNormalizedGeneric) {
  const activeTheme = useActiveUiTheme();
  const resolution = useState<number>("uiThemeResolution", () => 0);
  const resolutionId = ++resolution.value;
  const schema = Array.isArray(route.params.schema)
    ? route.params.schema[0]
    : route.params.schema;

  const [systemTheme, schemaTheme] = await Promise.all([
    fetchSystemTheme(),
    schema ? fetchSchemaTheme(schema, route) : Promise.resolve(""),
  ]);

  const theme = schemaTheme || systemTheme;
  if (resolutionId !== resolution.value) {
    return theme;
  }
  activeTheme.value = theme;

  // apply directly so the theme is in place before the page renders,
  // useHead (see useLayoutHead) keeps it in sync afterwards
  if (import.meta.client && theme && !TOGGLEABLE_THEMES.includes(theme)) {
    document.documentElement.setAttribute("data-theme", theme);
  }

  return theme;
}
