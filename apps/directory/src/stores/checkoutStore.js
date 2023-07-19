import { defineStore } from "pinia";
import { computed, ref } from "vue";
import { createBookmark } from "../functions/bookmarkMapper";
import { useFiltersStore } from "./filtersStore";

export const useCheckoutStore = defineStore("checkoutStore", () => {
  const filtersStore = useFiltersStore();
  const checkoutValid = ref(false);
  const cartUpdated = ref(false);

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
    const currentSelectionForBiobank =
      selectedCollections.value[biobankIdentifier];

    if (currentSelectionForBiobank && currentSelectionForBiobank.length) {
      const currentIds = currentSelectionForBiobank.map((sc) => sc.value);
      const newCollections = collections.filter(
        (cf) => !currentIds.includes(cf.value)
      );

      selectedCollections.value[
        biobankIdentifier
      ] = currentSelectionForBiobank.concat(newCollections);
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
        selectedCollections.value[
          biobankIdentifier
        ] = collectionSelectionForBiobank;
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

  return {
    checkoutValid,
    cartUpdated,
    selectedCollections,
    collectionSelectionCount,
    addCollectionsToSelection,
    removeCollectionsFromSelection,
    removeAllCollectionsFromSelection,
  };
});
