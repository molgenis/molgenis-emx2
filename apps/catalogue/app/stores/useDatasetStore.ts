import { fetchSettings } from "../composables/fetchSettings";
import { defineStore } from "pinia";
import { reactive, ref } from "vue";
import type { IResources } from "../../interfaces/catalogue";
import type { ISetting } from "../../../metadata-utils/src";

export const useDatasetStore = defineStore("datasets", () => {
  const datasets = reactive<Record<string, IResources>>({});
  const isEnabled = ref<boolean>(false);
  const datasetStoreUrl = ref<string>("");
  const storeVersion = ref<string>("");

  const CATALOGUE_STORE_IS_ENABLED = "CATALOGUE_STORE_IS_ENABLED";
  const CATALOGUE_STORE_URL = "CATALOGUE_STORE_URL";
  const CATALOGUE_STORE_VERSION = "CATALOGUE_STORE_VERSION";

  setStoreVariables();

  async function setStoreVariables() {
    const response = await fetchSettings([
      CATALOGUE_STORE_IS_ENABLED,
      CATALOGUE_STORE_URL,
      CATALOGUE_STORE_VERSION,
    ]);

    const settings = response.data._settings;

    const isEnabledSetting = findSetting(CATALOGUE_STORE_IS_ENABLED, settings);

    if (isEnabledSetting) {
      isEnabled.value = isEnabledSetting.value === "true";
      const urlSetting = findSetting(CATALOGUE_STORE_URL, settings);
      const versionSetting = findSetting(CATALOGUE_STORE_VERSION, settings);

      if (!urlSetting || !versionSetting) {
        throw new Error("Catalogue store URL or version setting not found");
      } else {
        datasetStoreUrl.value = urlSetting.value;
        storeVersion.value = versionSetting.value;
      }
    }
  }

  function findSetting(setting: string, settings: ISetting[]) {
    return settings.find(
      (sett: { key: string; value: string }) => sett.key === setting
    );
  }

  function addToCart(resource: IResources) {
    datasets[resource.id as keyof IResources] = resource;
  }

  function removeFromCart(resourceId: string) {
    delete datasets[resourceId as keyof IResources];
  }

  function resourceIsInCart(resourceId: string) {
    return !!datasets[resourceId as keyof IResources];
  }

  function clearCart() {
    for (const key in datasets) {
      delete datasets[key as keyof IResources];
    }
  }

  async function doNegotiatorV3Request() {
    const url = window.location.origin;
    const humanReadable = getHumanReadableString(datasets);
    const resources = toNegotiatorFormat(datasets);
    const payload: Record<string, any> = { url, humanReadable, resources };

    const response = await fetch(datasetStoreUrl.value, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(payload),
    });

    if (response.ok) {
      clearCart();
      const body = await response.json();
      window.location.href = body.redirectUrl;
    } else {
      return handleV3Error(response);
    }

    async function handleV3Error(response: Response) {
      const statusCode = response.status;
      const jsonResponse = await response.json();
      const detail = jsonResponse.detail
        ? ` Detail: ${jsonResponse.detail}`
        : "";
      switch (statusCode) {
        case 400:
          const error400 = `Negotiator responded with code 400, invalid input.${detail}`;
          console.error(error400);
          return error400;
        case 401:
          const error401 = `Negotiator responded with code 401, not authorised.${detail}`;
          console.error(error401);
          return error401;
        case 404:
          const error404 = `Negotiator not found, error code 404.${detail}`;
          console.error(error404);
          return error404;
        case 413:
          const error413 = `Negotiator responded with code 413, request too large.${detail}`;
          console.error(error413);
          return error413;
        case 500:
          const error500 = `Negotiator responded with code 500, internal server error.${detail}`;
          console.error(error500);
          return error500;
        default:
          const errorUnknown = `An unknown error occurred with the Negotiator. Please try again later.${detail}`;
          console.error(errorUnknown);
          return errorUnknown;
      }
    }

    function toNegotiatorFormat(datasets: Record<string, IResources>) {
      return Object.values(datasets).map((dataset) => ({
        id: dataset.pid,
        name: dataset.name,
      }));
    }

    function getHumanReadableString(datasets: Record<string, IResources>) {
      const datasetInfo = Object.values(datasets).map((dataset) => {
        return { pid: dataset.pid, name: dataset.name };
      });
      const humanReadableString = datasetInfo
        .reduce((acc, dataset) => {
          return acc + `${dataset.name} (${dataset.pid}), `;
        }, "")
        .slice(0, -2);

      return humanReadableString;
    }
  }

  async function doStoreRequest() {
    if (!Object.keys(datasets).length) {
      return;
    }

    switch (storeVersion.value) {
      case "REMS":
        window.open(datasetStoreUrl.value, "_blank");
        break;
      case "negotiatorV3":
        return await doNegotiatorV3Request();
      default:
        return "Unknown data store version, cannot send to store.";
    }
  }

  return {
    datasets,
    datasetStoreUrl,
    isEnabled,
    storeVersion,
    addToCart,
    clearCart,
    doStoreRequest,
    removeFromCart,
    resourceIsInCart,
  };
});
