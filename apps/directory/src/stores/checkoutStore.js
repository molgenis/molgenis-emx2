import { defineStore } from "pinia";
import { ref } from "vue";

export const useCheckoutStore = defineStore("checkoutStore", () => {
  const checkoutValid = ref(false);

  let selectedCollections = ref([]);

  function addCollectionsToSelection({ collections, bookmark }) {
    checkoutValid.value = false;
    const currentIds = selectedCollections.value.map((sc) => sc.value);
    const newCollections = collections.filter(
      (cf) => !currentIds.includes(cf.value)
    );
    selectedCollections.value = selectedCollections.value.concat(
      newCollections
    );

    // commit('SetSearchHistory', getters.getHumanReadableString)

    if (bookmark) {
      checkoutValid.value = true;
      //   createBookmark(state.filters, state.selectedCollections)
    }

    return { collections, bookmark };
  }

  function removeCollectionsFromSelection({ collections, bookmark }) {
    checkoutValid.value = false;
    const collectionsToRemove = collections.map((c) => c.value);
    selectedCollections.value = selectedCollections.value.filter(
      (sc) => !collectionsToRemove.includes(sc.value)
    );

    /** this means we are back are the home screen. */
    if (bookmark) {
      // TODO: figure out how this is impacted on other screens besides the biobankcard view.
      checkoutValid.value = true;
      // createBookmark(state.filters, state.selectedCollections)
    }
  }

  return {
    selectedCollections,
    addCollectionsToSelection,
    removeCollectionsFromSelection,
  };
});
