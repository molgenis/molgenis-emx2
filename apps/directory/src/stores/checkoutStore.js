import { defineStore } from "pinia";
import { computed, ref, toRaw } from "vue";
import { createBookmark } from "../functions/bookmarkMapper";
import { useFiltersStore } from "./filtersStore";
import { useSettingsStore } from "./settingsStore";

export const useCheckoutStore = defineStore("checkoutStore", () => {
  const filtersStore = useFiltersStore();
  const settingsStore = useSettingsStore();
  const checkoutValid = ref(false);
  const cartUpdated = ref(false);

  const biobankIdDictionary = ref({});

  let selectedCollections = ref({});

  const collectionSelectionCount = computed(() => {
    const allBiobanks = Object.keys(selectedCollections.value);
    let collectionCount = 0;

    for (const biobank of allBiobanks) {
      collectionCount += selectedCollections.value[biobank].length;
    }

    return collectionCount;
  });

  function addCollectionsToSelection({ biobank, collections, bookmark }) {
    checkoutValid.value = false;
    const biobankIdentifier = biobank.label || biobank.name;
    biobankIdDictionary.value[biobankIdentifier] = biobank.id;
    const currentSelectionForBiobank =
      selectedCollections.value[biobankIdentifier];

    if (currentSelectionForBiobank && currentSelectionForBiobank.length) {
      const currentIds = currentSelectionForBiobank.map((sc) => sc.value);
      const newCollections = collections.filter(
        (cf) => !currentIds.includes(cf.value)
      );

      selectedCollections.value[biobankIdentifier] =
        currentSelectionForBiobank.concat(newCollections);
    } else {
      selectedCollections.value[biobankIdentifier] = collections;
    }

    // commit('SetSearchHistory', getters.getHumanReadableString)

    if (bookmark) {
      checkoutValid.value = true;
      createBookmark(filtersStore.filters, selectedCollections.value);
    } else {
      /** we should not refresh on a cart update, so track this */
      cartUpdated.value = true;
    }

    return { collections, bookmark };
  }

  function removeCollectionsFromSelection({ biobank, collections, bookmark }) {
    checkoutValid.value = false;
    const biobankIdentifier = biobank.label || biobank.name;

    if (selectedCollections.value[biobankIdentifier]) {
      const collectionSelectionForBiobank =
        selectedCollections.value[biobankIdentifier];
      const collectionsToRemove = collections.map((c) => c.value);
      for (const collectionId of collectionsToRemove) {
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
      createBookmark(filtersStore.filters, selectedCollections.value);
    } else {
      /** we should not refresh on a cart update, so track this */
      cartUpdated.value = true;
    }
  }

  function removeAllCollectionsFromSelection({ bookmark }) {
    checkoutValid.value = false;

    selectedCollections.value = {};

    if (bookmark) {
      checkoutValid.value = true;
      createBookmark(filtersStore.filters, selectedCollections.value);
    }
  }

  function getHumanReadableString() {
    const activeFilterNames = Object.keys(filtersStore.filters);

    if (!activeFilterNames) return;

    let humanReadableString = "";
    const additionText = " and ";
    const humanReadableStart = {};

    /** Get all the filterdefinitions for current active filters and make a dictionary name: humanreadable */
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

  function sendToNegotiator() {
    const collections = [];

    for (const biobank in selectedCollections.value) {
      const collectionSelection = selectedCollections.value[biobank];
      for (const collection of collectionSelection) {
        collections.push(
          toRaw({
            collectionID: collection.value,
            biobankID: biobankIdDictionary.value[biobank],
          })
        );
      }
    }
    const URL = window.location.href.replace(/&nToken=\w{32}/, "");
    const humanReadable = getHumanReadableString();
    const negotiatorUrl = settingsStore.config.negotiatorUrl;

    const nToken = "";

    fetch(negotiatorUrl, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ URL, humanReadable, collections }),
    })
      .then(async (response) => {
        const body = await response.json();
        window.location.href = body.redirect_uri;
      })
      .catch(function (err) {
        console.info(err + " url: " + negotiatorUrl);
      });
  }

  return {
    checkoutValid,
    cartUpdated,
    sendToNegotiator,
    selectedCollections,
    collectionSelectionCount,
    addCollectionsToSelection,
    removeCollectionsFromSelection,
    removeAllCollectionsFromSelection,
  };
});
