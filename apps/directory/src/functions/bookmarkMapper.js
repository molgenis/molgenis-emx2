import router from "../router";
import { useFiltersStore } from "../stores/filtersStore";
import { useCollectionStore } from "../stores/collectionStore";
import { useCheckoutStore } from "../stores/checkoutStore";

let bookmarkApplied = false;

function setBookmark(bookmark) {
  bookmarkApplied = true;
  router.push({
    name: router.currentRoute.name,
    query: bookmark,
  });
}

export async function applyBookmark(watchedQuery) {
  if(bookmarkApplied) {
    bookmarkApplied = false;
    return;
  }


  const checkoutStore = useCheckoutStore();
  const collectionStore = useCollectionStore();
  const filtersStore = useFiltersStore();

  let query = watchedQuery;

  if (!query) {
    const route = router.currentRoute.value;

    if (!route.query) return;

    query = route.query;
  }

  if (!query || !Object.keys(query).length > 0) return;

  /**  negotiator token */
  // if (query.nToken) {
  //   state.nToken = query.nToken
  // }

  if (query.cart) {
    const decoded = decodeURIComponent(query.cart);
    const cartIdString = atob(decoded);
    const cartIds = cartIdString.split(",");

    const missingCollections = await collectionStore.getMissingCollectionInformation(
      cartIds
    );
    if (missingCollections && Object.keys(missingCollections).length) {
      for (const collection of missingCollections) {
        checkoutStore.addCollectionsToSelection({
          biobank: collection.biobank,
          collections: [{ label: collection.name, value: collection.id }],
        });
      }
    }

    /** add the beginning of history if from a link-back url */
    // if (state.searchHistory.length === 0) {
    //     state.searchHistory.push('Starting with a preselected list of collections')
    // }
  }

  /** we load the filters, grab the names, so we can loop over it to map the selections */
  const filters = Object.keys(filtersStore.facetDetails);

  if (query.matchAll) {
    const matchAllFilters = decodeURIComponent(query.matchAll).split(",");
    for (const filterName of matchAllFilters) {
      filtersStore.updateFilterType(filterName, "all", true);
    }
  }

  for (const filterName of filters) {
    if (query[filterName]) {
      let filtersToAdd = decodeURIComponent(query[filterName]);

      if (filterName === "Diagnosisavailable") {
        const diagnosisFacetDetails = filtersStore.facetDetails[filterName];
        /** the diagnosis available has been encoded, to discourage messing with the tree and breaking stuff. */
        const queryValues = atob(filtersToAdd).split(",");

        const options = await filtersStore.getOntologyOptionsForCodes(
          diagnosisFacetDetails,
          queryValues
        );
        filtersStore.updateOntologyFilter(filterName, options, true, true);
        /** retrieve these from the server, this is easier than tree traversal */
      } else {
        const filterOptions = filtersStore.filterOptionsCache[filterName];

        if (filterOptions) {
          const queryValues = filtersToAdd.split(",");
          filtersToAdd = filterOptions.filter((fo) =>
            queryValues.includes(fo.value)
          );
        } else {
          filtersToAdd = filtersToAdd === "true" ? true : filtersToAdd;
        }
        filtersStore.updateFilter(filterName, filtersToAdd, true);
      }
    }
  }
}
export function createBookmark(filters, collectionCart) {
  const filtersStore = useFiltersStore();
  const bookmark = {};
  const matchAll = [];

  if (filters) {
    const activeFilters = Object.keys(filters);
    for (const filterName of activeFilters) {
      let value = filters[filterName];

      /** can't do if(!value) because that would also trigger if value === 0 */
      if (
        value === "" ||
        value === null ||
        value === undefined ||
        value.length === 0
      ) {
        continue;
      }

      const filterType = filtersStore.getFilterType(filterName);

      if (filterType === "all") {
        matchAll.push(filterName);
      }

      if (Array.isArray(value) && value.length > 0) {
        const extractedValues = value.map(
          /** ontology / checkbox / other */
          (value) => value["code"] || value["value"] || value["name"]
        );

        let bookmarkValue = extractedValues.join(",");

        /** you may not alter this in the url, because that will mess up the tree.
         * discourage this behaviour by encoding it.
         */
        if (filterName === "Diagnosisavailable") {
          bookmarkValue = btoa(bookmarkValue);
        }

        bookmark[filterName] = encodeURI(bookmarkValue);
      } else if (typeof value === "object") {
        let bookmarkValue = value["code"] || value["value"] || value["name"];

        if (filterName === "Diagnosisavailable") {
          bookmarkValue = btoa(bookmarkValue);
        }

        bookmark[filterName] = encodeURI();
      } else {
        let bookmarkValue = value;
        if (filterName === "Diagnosisavailable") {
          bookmarkValue = btoa(bookmarkValue);
        }

        bookmark[filterName] = encodeURI(value);
      }
    }
  }

  /** This manages the selection in the cart */
  if (collectionCart && Object.keys(collectionCart).length) {
    const bookmarkIds = [];
    for (const biobank in collectionCart) {
      bookmarkIds.push(collectionCart[biobank].map((s) => s.value));
    }

    const encodedCart = btoa(bookmarkIds.join(","));
    bookmark.cart = encodeURI(encodedCart);
  }

  if (matchAll.length) {
    bookmark.matchAll = encodeURI(matchAll.join(","));
  }

  if (!filtersStore.bookmarkWaitingForApplication) {
    setBookmark(bookmark);
  }
}

export default {
  createBookmark,
};
