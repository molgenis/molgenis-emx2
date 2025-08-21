import * as _ from "lodash";
import { defineStore } from "pinia";
import { computed, ref, toRaw, watch } from "vue";
import { createBookmark } from "../functions/bookmarkMapper";
import { IBiobanks } from "../interfaces/directory";
import { IBiobankIdentifier } from "../interfaces/interfaces";
import { useFiltersStore } from "./filtersStore";
import { useSettingsStore } from "./settingsStore";
import useErrorHandler from "../composables/errorHandler";

export interface ILabelValuePair {
  label: string;
  value: string;
}

const { setError, clearError } = useErrorHandler();
const NEGOTIATOR_ERROR =
  "An error occurred while communicating with the Negotiator. Please try again later.";

export const useCheckoutStore = defineStore("checkoutStore", () => {
  const filtersStore = useFiltersStore();
  const settingsStore = useSettingsStore();
  const checkoutValid = ref(false);
  const cartUpdated = ref(false);

  const searchHistory = ref<string[]>([]);
  const nToken = ref("");

  const biobankIdDictionary = ref<Record<string, string>>({});

  const selectedCollections = ref<
    Record<string, { label: string; value: string }[]>
  >({});
  const selectedServices = ref<
    Record<string, { label: string; value: string }[]>
  >({});

  const serializedSelectedCollections = localStorage.getItem(
    "selectedCollections"
  );

  const serializedSelectedServices = localStorage.getItem("selectedServices");

  if (serializedSelectedCollections) {
    const deserializedSelectedCollections = JSON.parse(
      serializedSelectedCollections
    );
    selectedCollections.value = deserializedSelectedCollections;
  }

  if (serializedSelectedServices) {
    const deserializedSelectedServices = JSON.parse(serializedSelectedServices);
    selectedServices.value = deserializedSelectedServices;
  }

  watch(
    selectedCollections,
    (newSelectedCollections) => {
      localStorage.setItem(
        "selectedCollections",
        JSON.stringify(toRaw(newSelectedCollections))
      );
    },
    { deep: true }
  );

  watch(
    selectedServices,
    (newSelectedServices) => {
      localStorage.setItem(
        "selectedServices",
        JSON.stringify(toRaw(newSelectedServices))
      );
    },
    { deep: true }
  );

  const collectionSelectionCount = computed(() => {
    const allBiobanks = Object.keys(selectedCollections.value);
    return allBiobanks.reduce((accum, biobank) => {
      return accum + selectedCollections.value[biobank].length;
    }, 0);
  });

  const serviceSelectionCount = computed(() => {
    const allBiobanks = Object.keys(selectedServices.value);
    return allBiobanks.reduce((accum, biobank) => {
      return accum + selectedServices.value[biobank].length;
    }, 0);
  });

  function setSearchHistory(history: string) {
    if (history === "") {
      history = "No filters used.";
    }

    /** only add if this is a different query than before */
    if (searchHistory.value![searchHistory.value.length - 1] !== history) {
      searchHistory.value.push(history);
    }
  }

  function addServicesToSelection(
    biobank: IBiobanks,
    services: ILabelValuePair[],
    bookmark: boolean
  ) {
    checkoutValid.value = false;
    const biobankIdentifier = biobank.name;
    biobankIdDictionary.value[biobankIdentifier] = biobank.id;
    const currentSelectionForBiobank =
      selectedServices.value[biobankIdentifier];

    if (currentSelectionForBiobank && currentSelectionForBiobank.length) {
      const currentIds = currentSelectionForBiobank.map((sc) => sc.value);
      const newServices = services.filter(
        (cf) => !currentIds.includes(cf.value)
      );

      setSearchHistory(
        `Selected ${newServices
          .map((nc) => nc.label)
          .join(", ")} from ${biobankIdentifier}`
      );

      selectedServices.value[biobankIdentifier] =
        currentSelectionForBiobank.concat(newServices);
    } else {
      selectedServices.value[biobankIdentifier] = services;

      setSearchHistory(
        `Selected ${services
          .map((nc) => nc.label)
          .join(", ")} from ${biobankIdentifier}`
      );

      if (bookmark) {
        checkoutValid.value = true;
        // todo need to add service stuff to the bookmark
        createBookmark(
          filtersStore.filters,
          selectedCollections.value,
          selectedServices.value,
          filtersStore.filterType
        );
      } else {
        /** we should not refresh on a cart update, so track this */
        cartUpdated.value = true;
      }

      return { services, bookmark };
    }
  }

  function addCollectionsToSelection(
    biobank: IBiobanks,
    collections: ILabelValuePair[],
    bookmark: boolean
  ) {
    checkoutValid.value = false;
    const biobankIdentifier = biobank.name;
    biobankIdDictionary.value[biobankIdentifier] = biobank.id;
    const currentSelectionForBiobank =
      selectedCollections.value[biobankIdentifier];

    if (currentSelectionForBiobank && currentSelectionForBiobank.length) {
      const currentIds = currentSelectionForBiobank.map((sc) => sc.value);
      const newCollections = collections.filter(
        (cf) => !currentIds.includes(cf.value)
      );

      setSearchHistory(
        `Selected ${newCollections
          .map((collection) => collection.label)
          .join(", ")} from ${biobankIdentifier}`
      );

      selectedCollections.value[biobankIdentifier] =
        currentSelectionForBiobank.concat(newCollections);
    } else {
      selectedCollections.value[biobankIdentifier] = collections;

      setSearchHistory(
        `Selected ${collections
          .map((collection) => collection.label)
          .join(", ")} from ${biobankIdentifier}`
      );
    }

    if (bookmark) {
      checkoutValid.value = true;
      createBookmark(
        filtersStore.filters,
        selectedCollections.value,
        selectedServices.value,
        filtersStore.filterType
      );
    } else {
      /** we should not refresh on a cart update, so track this */
      cartUpdated.value = true;
    }

    return { collections, bookmark };
  }

  function removeServicesFromSelection(
    biobank: IBiobankIdentifier,
    serviceIds: string[],
    bookmark: boolean
  ) {
    checkoutValid.value = false;
    const biobankIdentifier = biobank.name;

    if (selectedServices.value[biobankIdentifier]) {
      const serviceSelectionForBiobank =
        selectedServices.value[biobankIdentifier];
      for (const serviceId of serviceIds) {
        const getRemoveIdIndex = serviceSelectionForBiobank.findIndex(
          (service) => service.value === serviceId
        );

        if (getRemoveIdIndex < 0) {
          break;
        } else {
          serviceSelectionForBiobank.splice(getRemoveIdIndex, 1);
        }
      }

      if (serviceSelectionForBiobank.length) {
        selectedServices.value[biobankIdentifier] = serviceSelectionForBiobank;
      } else {
        delete selectedServices.value[biobankIdentifier];
      }
    }

    if (bookmark) {
      checkoutValid.value = true;
      createBookmark(
        filtersStore.filters,
        selectedCollections.value,
        selectedServices.value,
        filtersStore.filterType
      );
    } else {
      /** we should not refresh on a cart update, so track this */
      cartUpdated.value = true;
    }
  }

  function removeCollectionsFromSelection(
    biobank: IBiobankIdentifier,
    collectionIds: string[],
    bookmark: boolean
  ) {
    checkoutValid.value = false;
    const biobankIdentifier = biobank.name;

    if (selectedCollections.value[biobankIdentifier]) {
      const collectionSelectionForBiobank =
        selectedCollections.value[biobankIdentifier];

      for (const collectionId of collectionIds) {
        const getRemoveIdIndex = collectionSelectionForBiobank.findIndex(
          (collection) => collection.value === collectionId
        );

        if (getRemoveIdIndex < 0) {
          break;
        } else {
          collectionSelectionForBiobank.splice(getRemoveIdIndex, 1);
        }
      }

      if (collectionSelectionForBiobank.length) {
        selectedCollections.value[biobankIdentifier] =
          collectionSelectionForBiobank;
      } else {
        delete selectedCollections.value[biobankIdentifier];
      }
    }

    if (bookmark) {
      checkoutValid.value = true;
      createBookmark(
        filtersStore.filters,
        selectedCollections.value,
        selectedServices.value,
        filtersStore.filterType
      );
    } else {
      /** we should not refresh on a cart update, so track this */
      cartUpdated.value = true;
    }
  }

  function removeAllFromSelection(bookmark: boolean) {
    checkoutValid.value = false;

    selectedCollections.value = {};
    selectedServices.value = {};

    if (bookmark) {
      checkoutValid.value = true;
      createBookmark(
        filtersStore.filters,
        selectedCollections.value,
        selectedServices.value,
        filtersStore.filterType
      );
    }
  }

  function getHumanReadableString() {
    const activeFilterNames = Object.keys(filtersStore.filters);

    if (!activeFilterNames) return;

    let humanReadableString = "";
    const additionText = " and ";
    const humanReadableStart: Record<string, string> = {};

    /** Get all the filter definitions for current active filters and make a dictionary name: humanreadable */
    filtersStore.filterFacets
      .filter((fd) => activeFilterNames.includes(fd.facetIdentifier))
      .forEach((filterDefinition) => {
        humanReadableStart[filterDefinition.facetIdentifier] =
          filterDefinition.negotiatorRequestString;
      });

    for (const [filterName, filterValue] of Object.entries(
      filtersStore.filters
    )) {
      if (!filterValue) continue;
      if (!Array.isArray(filterValue)) continue;
      humanReadableString += humanReadableStart[filterName];

      if (filterName === "search") {
        humanReadableString += ` ${filterValue}`;
      } else {
        humanReadableString += ` ${filterValue
          .map((fv) => fv.text)
          .join(", ")}`;
      }
      humanReadableString += additionText;
    }

    if (humanReadableString === "") return humanReadableString;

    return humanReadableString.substring(
      0,
      humanReadableString.length - additionText.length
    );
  }

  function createHistoryJournal() {
    let journal = "";

    for (let i = 0, length = searchHistory.value.length; i < length; i++) {
      journal += `#${i + 1}: ${searchHistory.value[i]}\r\n`;
    }
    return (
      ". User actions taken: " + journal.substring(0, journal.length - 2)
    ); /** remove the last \r\n */
  }

  async function sendToNegotiator() {
    const { negotiatorType } = settingsStore.config;
    clearError();
    if (negotiatorType === "v1") {
      doNegotiatorV1Request();
    } else if (
      negotiatorType === "v3" ||
      negotiatorType === "eric-negotiator"
    ) {
      doNegotiatorV3Request();
    } else {
      console.error(
        `Unsupported negotiator type: ${negotiatorType}. Please check your settings.`
      );
      setError(NEGOTIATOR_ERROR);
    }
  }

  async function doNegotiatorV1Request() {
    const { negotiatorUsername, negotiatorPassword, negotiatorUrl } =
      settingsStore.config;
    const humanReadable = getHumanReadableString() + createHistoryJournal();
    const collections: any[] = getV1CollectionsToSend();
    const URL = window.location.href.replace(/&nToken=\w{32}/, "");
    const body: Record<string, any> = {
      podiumUrl: negotiatorUrl,
      podiumUsername: negotiatorUsername,
      podiumPassword: negotiatorPassword,
      payload: { URL, humanReadable, collections },
    };

    if (nToken.value) {
      body.nToken = nToken.value;
    }

    const response = await fetch(`/api/podium`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(body),
    });

    if (response.ok) {
      removeAllFromSelection(false);
      if (typeof response.headers.get("Location") === "string") {
        window.location.href = response.headers.get("Location") as string;
      }
    } else {
      const jsonResponse = await response.json();
      const detail = jsonResponse.detail
        ? ` Detail: ${jsonResponse.detail}`
        : "";
      const statusString = response.status
        ? ` Status: ${response.status} (${response.statusText}).`
        : "";
      console.error(
        `Error communicating with the Negotiator: ${statusString} ${detail}`
      );
      setError(NEGOTIATOR_ERROR);
    }
  }

  async function doNegotiatorV3Request() {
    const { negotiatorUrl } = settingsStore.config;
    const humanReadable = getHumanReadableString() + createHistoryJournal();

    const resources = getResourcesToSend();
    const url = window.location.origin;
    const payload: Record<string, any> = { url, humanReadable, resources };
    if (nToken.value) {
      payload.nToken = nToken.value;
    }
    const response = await fetch(negotiatorUrl, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(payload),
    });

    if (response.ok) {
      removeAllFromSelection(false);
      const body = await response.json();
      window.location.href = body.redirectUrl;
    } else {
      const statusCode = response.status;
      const jsonResponse = await response.json();
      const detail = jsonResponse.detail
        ? ` Detail: ${jsonResponse.detail}`
        : "";
      setError(NEGOTIATOR_ERROR);
      switch (statusCode) {
        case 400:
          console.error(
            `Negotiator responded with code 400, invalid input.${detail}`
          );
        case 401:
          console.error(
            `Negotiator responded with code 401, not authorised.${detail}`
          );
        case 404:
          console.error(`Negotiator not found, error code 404.${detail}`);
        case 413:
          console.error(
            `Negotiator responded with code 413, request too large.${detail}`
          );
        case 500:
          console.error(
            `Negotiator responded with code 500, internal server error.${detail}`
          );
        default:
          console.error(
            `An unknown error occurred with the Negotiator. Please try again later.${detail}`
          );
      }
    }
  }

  function getV1CollectionsToSend() {
    const selectedCollectionsByBiobank = selectedCollections.value;
    return _.flatMap(
      selectedCollectionsByBiobank,
      (collectionSelection, biobankName) => {
        return collectionSelection.map((collection) => {
          return toRaw({
            collectionId: collection.value,
            biobankId: biobankIdDictionary.value[biobankName],
          });
        });
      }
    );
  }

  function getResourcesToSend() {
    const collections = getCollectionsToSend(selectedCollections.value);
    const services = getServicesToSend(selectedServices.value);
    return [...collections, ...services];
  }

  function getCollectionsToSend(
    selectedCollectionsByBiobank: Record<string, ILabelValuePair[]>
  ) {
    return _.flatMap(selectedCollectionsByBiobank, (collectionSelection) => {
      return collectionSelection.map((collection) => {
        return toRaw({ id: collection.value, name: collection.label });
      });
    });
  }

  function getServicesToSend(
    selectedServices: Record<string, ILabelValuePair[]>
  ) {
    return _.flatMap(selectedServices, (serviceSelection) => {
      return serviceSelection.map((service) => {
        return toRaw({ id: service.value, name: service.label });
      });
    });
  }

  function isInCart(identifier: string) {
    for (const biobankName in selectedCollections.value) {
      const collectionSelection = selectedCollections.value[biobankName];

      for (const collection of collectionSelection) {
        if (collection.value === identifier) {
          return true;
        }
      }
    }

    for (const biobankName in selectedServices.value) {
      const serviceSelection = selectedServices.value[biobankName];

      for (const service of serviceSelection) {
        if (service.value === identifier) {
          return true;
        }
      }
    }

    return false;
  }

  return {
    checkoutValid,
    collectionSelectionCount,
    cartUpdated,
    nToken,
    searchHistory,
    selectedCollections,
    selectedServices,
    serviceSelectionCount,
    addCollectionsToSelection,
    addServicesToSelection,
    isInCart,
    removeAllFromSelection,
    removeCollectionsFromSelection,
    removeServicesFromSelection,
    sendToNegotiator,
    setSearchHistory,
  };
});
