import { fetchSettings } from "../../../../tailwind-components/app/utils/fetchSettings";
import type { ISetting } from "../../../../metadata-utils/src/types";

const CATALOGUE_STORE_IS_ENABLED = "CATALOGUE_STORE_IS_ENABLED";
const CATALOGUE_STORE_URL = "CATALOGUE_STORE_URL";
const CATALOGUE_STORE_VERSION = "CATALOGUE_STORE_VERSION";

export async function getCatalogueStoreConfig() {
  const response = await fetchSettings([
    CATALOGUE_STORE_IS_ENABLED,
    CATALOGUE_STORE_URL,
    CATALOGUE_STORE_VERSION,
  ]);

  const settings = response.data._settings;
  const isEnabledSetting = findSetting(CATALOGUE_STORE_IS_ENABLED, settings);
  const urlSetting = findSetting(CATALOGUE_STORE_URL, settings);
  const versionSetting = findSetting(CATALOGUE_STORE_VERSION, settings);

  if (isEnabledSetting?.value === "true") {
    if (!urlSetting?.value || !versionSetting?.value) {
      throw new Error("Catalogue store URL or version setting not found");
    } else {
      return {
        enabled: true,
        url: urlSetting.value,
        version: versionSetting.value,
      };
    }
  } else {
    return {
      enabled: false,
      url: "",
      version: "",
    };
  }
}

function findSetting(setting: string, settings: ISetting[]) {
  return settings.find(
    (sett: { key: string; value: string }) => sett.key === setting
  );
}
