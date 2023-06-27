import { defineStore } from "pinia";
import { computed, ref } from "vue";

export const useCheckoutStore = defineStore("checkoutStore", () => {
  const checkoutValid = ref(false);

  let selectedCollections = ref({});

  const collectionSelectionCount = computed(() => {
    const allBiobanks = Object.keys(selectedCollections.value)
    let collectionCount = 0;

    for (const biobank of allBiobanks) {
      collectionCount += selectedCollections.value[biobank].length
    }

    return collectionCount;
  })

  function addCollectionsToSelection ({ biobank, collections, bookmark }) {
    // checkoutValid.value = false;
    const biobankIdentifier = biobank.label || biobank.name
    const currentSelectionForBiobank = selectedCollections.value[biobankIdentifier]

    if (currentSelectionForBiobank && currentSelectionForBiobank.length) {
      const currentIds = currentSelectionForBiobank.map((sc) => sc.value);
      const newCollections = collections.filter(
        (cf) => !currentIds.includes(cf.value)
      );

      selectedCollections.value[biobankIdentifier] = currentSelectionForBiobank.concat(
        newCollections)
    }
    else {
      selectedCollections.value[biobankIdentifier] = collections
    }

    // commit('SetSearchHistory', getters.getHumanReadableString)

    if (bookmark) {
      // checkoutValid.value = true;
      //   createBookmark(state.filters, state.selectedCollections)
    }

    return { collections, bookmark };
  }

  function removeCollectionsFromSelection ({ biobank, collections, bookmark }) {
    checkoutValid.value = false;
    const biobankIdentifier = biobank.label || biobank.name

    if (selectedCollections.value[biobankIdentifier]) {

      const collectionSelectionForBiobank = selectedCollections.value[biobankIdentifier]
      const collectionsToRemove = collections.map((c) => c.value);
      for (const collectionId of collectionsToRemove) {
        const getRemoveIdIndex = collectionSelectionForBiobank.findIndex(collection => collection.value === collectionId)

        if (getRemoveIdIndex < 0) {
          break
        }
        else {
          collectionSelectionForBiobank.splice(getRemoveIdIndex, 1)
        }
      }

      if (collectionSelectionForBiobank.length) {
        selectedCollections.value[biobankIdentifier] = collectionSelectionForBiobank
      }
      else {
        delete selectedCollections.value[biobankIdentifier]
      }
    }

    if (bookmark) {
      // TODO: figure out how this is impacted on other screens besides the biobankcard view.
      checkoutValid.value = true;
      // createBookmark(state.filters, state.selectedCollections)
    }
  }

  function removeAllCollectionsFromSelection ({ bookmark }) {
    checkoutValid.value = false;

    selectedCollections.value = {}

    if (bookmark) {
      // TODO: figure out how this is impacted on other screens besides the biobankcard view.
      checkoutValid.value = true;
      // createBookmark(state.filters, state.selectedCollections)
    }
  }

  return {
    selectedCollections,
    collectionSelectionCount,
    addCollectionsToSelection,
    removeCollectionsFromSelection,
    removeAllCollectionsFromSelection
  };
});
