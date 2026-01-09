import { useState } from "#app/composables/state";
import { ref } from "vue";
import type { Resp } from "../../types/types";
import { $fetch } from "ofetch";

export interface Settings {
  isOidcEnabled: boolean;
}

export interface Manifest {
  SpecificationVersion: string;
  DatabaseVersion: string;
}

const settings = ref<Settings | null>();
const manifest = ref<Manifest | null>(null);

async function fetchServerSettings() {
  return await $fetch<Resp<{ _settings: { key: string; value: any }[] }>>(
    "/api/graphql",
    {
      method: "POST",
      body: JSON.stringify({
        query: `{
          _settings (keys: ["isOidcEnabled"]){ key, value }
          _manifest { SpecificationVersion,DatabaseVersion }
        }`,
      }),
    }
  );
}

export const useSettings = async () => {
  if (!settings.value) {
    await fetchServerSettings().then((response) => {
      const data = Array.isArray(response.data)
        ? response.data[0]
        : response.data;
      const settingsArray: { key: string; value: any }[] = data._settings;
      const isOidcEnabledSetting = settingsArray.find(
        (item) => item.key === "isOidcEnabled"
      );
      settings.value = {
        isOidcEnabled: isOidcEnabledSetting
          ? isOidcEnabledSetting.value === true ||
            isOidcEnabledSetting.value === "true"
          : false,
      };
      manifest.value = data._manifest;
    });
  }
  return {
    settings: useState("settings", () => settings),
    manifest: useState("manifest", () => manifest),
  };
};
