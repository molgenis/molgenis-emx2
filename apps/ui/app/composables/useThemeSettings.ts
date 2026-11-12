import { computed, watch } from "vue";
import { useState } from "#app/composables/state";
import { useConfiguredTheme } from "../../../tailwind-components/app/composables/useConfiguredTheme";
import {
  applyThemeOverrides,
  parseCustomThemes,
  resolveTheme,
  type ICustomTheme,
  type IThemeLevel,
} from "../../../tailwind-components/app/utils/themes";

interface IThemeLevels {
  app?: IThemeLevel;
  schemaId?: string;
  schema?: IThemeLevel;
}

interface IGraphqlResponse<T> {
  data?: T;
  errors?: { message: string }[];
}

const ACTIVE_THEME_KEY = "tw-theme";
const CUSTOM_THEMES_KEY = "tw-custom-themes";
const EMPTY_LEVEL: IThemeLevel = { customThemes: [] };

function graphqlUrl(schema?: string): string {
  return schema ? `/${encodeURIComponent(schema)}/graphql` : "/api/graphql";
}

async function postGraphql<T>(
  schema: string | undefined,
  query: string,
  variables?: Record<string, unknown>
): Promise<T | undefined> {
  const response = await $fetch<IGraphqlResponse<T>>(graphqlUrl(schema), {
    method: "POST",
    body: { query, variables },
  });
  if (response.errors?.length) {
    throw new Error(response.errors[0]?.message);
  }
  return response.data;
}

async function fetchThemeLevel(schema?: string): Promise<IThemeLevel> {
  const data = await postGraphql<{
    _settings: { key: string; value: string }[];
  }>(
    schema,
    `{_settings(keys: ["${ACTIVE_THEME_KEY}", "${CUSTOM_THEMES_KEY}"]) { key value }}`
  );
  const settings = data?._settings ?? [];
  const valueOf = (key: string) =>
    settings.find((setting) => setting.key === key)?.value;
  return {
    active: valueOf(ACTIVE_THEME_KEY),
    customThemes: parseCustomThemes(valueOf(CUSTOM_THEMES_KEY)),
  };
}

// a theme that cannot be loaded must never get in the way of the application
async function fetchThemeLevelOrEmpty(schema?: string): Promise<IThemeLevel> {
  try {
    return await fetchThemeLevel(schema);
  } catch (error) {
    console.error(
      `Could not load theme settings for ${schema ?? "app"}`,
      error
    );
    return EMPTY_LEVEL;
  }
}

async function changeSetting(
  schema: string | undefined,
  key: string,
  value: string
) {
  await postGraphql(
    schema,
    `mutation change($settings: [MolgenisSettingsInput]) {
      change(settings: $settings) { message }
    }`,
    { settings: [{ key, value }] }
  );
}

async function dropSetting(schema: string | undefined, key: string) {
  await postGraphql(
    schema,
    `mutation drop($settings: [DropSettingsInput]) {
      drop(settings: $settings) { message }
    }`,
    { settings: [{ key }] }
  );
}

export function useThemeSettings(schema?: string) {
  const levels = useState<IThemeLevels>("themeLevels", () => ({}));
  const configuredTheme = useConfiguredTheme();

  const own = computed<IThemeLevel>(
    () => (schema ? levels.value.schema : levels.value.app) ?? EMPTY_LEVEL
  );
  const inherited = computed<IThemeLevel>(() =>
    schema ? levels.value.app ?? EMPTY_LEVEL : EMPTY_LEVEL
  );

  async function load(force = false) {
    const cached = levels.value;
    const [app, schemaLevel] = await Promise.all([
      cached.app && !force ? cached.app : fetchThemeLevelOrEmpty(),
      !schema || (cached.schemaId === schema && !force)
        ? cached.schema
        : fetchThemeLevelOrEmpty(schema),
    ]);
    levels.value = { app, schemaId: schema, schema: schemaLevel };
  }

  function applySavedTheme(query?: unknown) {
    configuredTheme.value = resolveTheme({
      query,
      schema: schema ? levels.value.schema : undefined,
      app: levels.value.app,
    });
  }

  async function setActiveTheme(id: string | undefined) {
    if (id) {
      await changeSetting(schema, ACTIVE_THEME_KEY, id);
    } else {
      await dropSetting(schema, ACTIVE_THEME_KEY);
    }
    await load(true);
  }

  async function writeCustomThemes(themes: ICustomTheme[]) {
    await changeSetting(schema, CUSTOM_THEMES_KEY, JSON.stringify(themes));
    await load(true);
  }

  // works from the latest stored list, so a save does not undo someone else's
  async function saveCustomTheme(theme: ICustomTheme) {
    const { customThemes } = await fetchThemeLevel(schema);
    const exists = customThemes.some((other) => other.id === theme.id);
    await writeCustomThemes(
      exists
        ? customThemes.map((other) => (other.id === theme.id ? theme : other))
        : [...customThemes, theme]
    );
  }

  async function deleteCustomTheme(id: string) {
    const { active, customThemes } = await fetchThemeLevel(schema);
    if (active === id) {
      throw new Error("This theme is in use. Select another theme first.");
    }
    await writeCustomThemes(customThemes.filter((theme) => theme.id !== id));
  }

  return {
    own,
    inherited,
    load,
    applySavedTheme,
    setActiveTheme,
    saveCustomTheme,
    deleteCustomTheme,
  };
}

export function useApplyConfiguredTheme() {
  const configuredTheme = useConfiguredTheme();
  let applied: string[] = [];
  watch(
    configuredTheme,
    (theme) => {
      applied = applyThemeOverrides(
        document.documentElement,
        theme?.overrides ?? {},
        applied
      );
    },
    { immediate: true }
  );
}

export function themeErrorMessage(error: unknown): string {
  const fetchError = error as {
    data?: { errors?: { message?: string }[] };
    message?: string;
  };
  return (
    fetchError?.data?.errors?.[0]?.message ??
    fetchError?.message ??
    "The theme could not be saved"
  );
}
