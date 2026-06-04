import { useState } from "#app/composables/state";
import { ref } from "vue";
import type { Resp } from "../../types/types";
import { $fetch } from "ofetch";

export interface Settings {
  isOidcEnabled: boolean;
  [key: string]: unknown;
}

async function fetchServerSettings(keys?: Set<string>) {
  const defaultKeys = ["isOidcEnabled"];
  const queryKeys = keys ? defaultKeys.concat(Array.from(keys)) : defaultKeys;
  return await $fetch<Resp<{ _settings: { key: string; value: any }[] }>>(
    "/api/graphql",
    {
      method: "POST",
      body: JSON.stringify({
        query: `{_settings (keys: [${Array.from(queryKeys)
          .map((k) => `"${k}"`)
          .join(",")}]){ key, value }}`,
      }),
    }
  );
}

export const useSettings = async (keys?: Set<string>) => {
  const settings = useState("settings", () => null as Settings | null);

  if (
    (settings.value && keys === undefined) ||
    (settings.value &&
      keys &&
      keys.size > 0 &&
      Array.from(keys).every((key) => settings.value![key]))
  ) {
    return settings;
  }

  await fetchServerSettings(keys).then((response) => {
    const settingsArray: { key: string; value: any }[] = Array.isArray(
      response.data
    )
      ? response.data[0]._settings
      : response.data._settings;
    const isOidcEnabledSetting = settingsArray.find(
      (item) => item.key === "isOidcEnabled"
    );
    settings.value = {
      isOidcEnabled: isOidcEnabledSetting
        ? isOidcEnabledSetting.value === true ||
          isOidcEnabledSetting.value === "true"
        : false,
    };

    if (keys) {
      keys.forEach((key) => {
        const setting = settingsArray.find((item) => item.key === key);
        if (setting) {
          settings.value![key as keyof Settings] = setting.value;
        }
      });
    }
  });

  return useState("settings", () => settings);
};
