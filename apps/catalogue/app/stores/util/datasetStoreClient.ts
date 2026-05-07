import type { IResources } from "../../../interfaces/catalogue";
import { fetchSettings } from "../../composables/fetchSettings";
import {
  findSetting,
  getHumanReadableString,
  toNegotiatorFormat,
} from "./datasetStoreUtils";

const CATALOGUE_STORE_IS_ENABLED = "CATALOGUE_STORE_IS_ENABLED";
const CATALOGUE_STORE_URL = "CATALOGUE_STORE_URL";
const CATALOGUE_STORE_VERSION = "CATALOGUE_STORE_VERSION";

export async function getStoreVariables() {
  const response = await fetchSettings([
    CATALOGUE_STORE_IS_ENABLED,
    CATALOGUE_STORE_URL,
    CATALOGUE_STORE_VERSION,
  ]);

  const settings = response.data._settings;
  const isEnabledSetting = findSetting(CATALOGUE_STORE_IS_ENABLED, settings);
  const urlSetting = findSetting(CATALOGUE_STORE_URL, settings);
  const versionSetting = findSetting(CATALOGUE_STORE_VERSION, settings);

  if (isEnabledSetting && (!urlSetting || !versionSetting)) {
    throw new Error("Catalogue store URL or version setting not found");
  } else {
    return {
      enabled: isEnabledSetting?.value === "true",
      url: urlSetting?.value || "",
      version: versionSetting?.value || "",
    };
  }
}

export async function doNegotiatorV3Request(
  datasets: Record<string, IResources>,
  datasetStoreUrl: string
) {
  const url = window.location.origin;
  const humanReadable = getHumanReadableString(datasets);
  const resources = toNegotiatorFormat(datasets);
  const payload: Record<string, any> = { url, humanReadable, resources };

  return await fetch(datasetStoreUrl, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });
}
