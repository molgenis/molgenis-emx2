import { useState } from "#app/composables/state";
import { ref } from "vue";
import type { Resp } from "../../types/types";
import { $fetch } from "ofetch";

export interface Settings {
  isOidcEnabled: boolean;
}

const settings = ref<Settings | null>();

async function fetchServerSettings() {
  return await $fetch<Resp<{ _settings: { key: string; value: string }[] }>>(
    "/api/graphql",
    {
      method: "POST",
      body: JSON.stringify({
        query: `{_settings (keys: ["isOidcEnabled"]){ key, value }}`,
      }),
    }
  );
}

export const useSettings = async () => {
  if (!settings.value) {
    await fetchServerSettings().then((response) => {
      const settingsArray: { key: string; value: string }[] = Array.isArray(
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
    });
  }

  return useState("settings", () => settings);
};
