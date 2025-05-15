import { LocationQuery } from "vue-router";
import useErrorHandler from "../composables/errorHandler";
import router from "../router";
import { labelValuePair, useCheckoutStore } from "../stores/checkoutStore";
import { useCollectionStore } from "../stores/collectionStore";
import { useFiltersStore } from "../stores/filtersStore";
import { IOntologyItem } from "../interfaces/interfaces";
import { forEach } from "lodash";
let bookmarkApplied = false;

const { setError } = useErrorHandler();

export async function applyBookmark(watchedQuery: LocationQuery) {
  if (bookmarkApplied) {
    bookmarkApplied = false;
    return;
  }

  if (!watchedQuery || !(Object.keys(watchedQuery).length > 0)) {
    return;
  }

  const checkoutStore = useCheckoutStore();
  const collectionStore = useCollectionStore();
  const filtersStore = useFiltersStore();

  /**  negotiator token */
  if (watchedQuery.nToken) {
    checkoutStore.nToken = watchedQuery.nToken as string;
  }

  if (watchedQuery.cart) {
    const decoded = decodeURIComponent(watchedQuery.cart as string);
    const cartIdString = atob(decoded);
    const cartIds = cartIdString.split(",");
    const missingCollections =
      await collectionStore.getMissingCollectionInformation(cartIds);
    if (missingCollections && Object.keys(missingCollections).length) {
      for (const collection of missingCollections) {
        checkoutStore.addCollectionsToSelection(
          collection.biobank,
          [{ label: collection.name, value: collection.id }],
          false
        );
      }
    }

    /** add the beginning of history if from a link-back url */
    if (checkoutStore.searchHistory.length === 0) {
      checkoutStore.searchHistory.push(
        "Starting with a preselected list of collections"
      );
    }
  }
  /** we load the filters, grab the names, so we can loop over it to map the selections */
  const filters = Object.keys(filtersStore.facetDetails);
  if (watchedQuery.matchAll) {
    const matchAllFilters = decodeURIComponent(
      watchedQuery.matchAll as string
    ).split(",");
    for (const filterName of matchAllFilters) {
      filtersStore.updateFilterType(filterName, "all", true);
    }
  }

  for (const filterName of filters) {
    if (watchedQuery[filterName]) {
      const filtersToAdd: string = decodeURIComponent(
        watchedQuery[filterName] as string
      );

      if (filterName === "Diagnosisavailable") {
        const diagnosisFacetDetails = filtersStore.facetDetails[filterName];
        /** the diagnosis available has been encoded, to discourage messing with the tree and breaking stuff. */
        const queryValues = atob(filtersToAdd).split(",");
        const options: IOntologyItem[] =
          await filtersStore.getOntologyOptionsForCodes(
            diagnosisFacetDetails,
            queryValues
          );
        options.forEach((option) => {
          filtersStore.updateOntologyFilter(filterName, option, true, true);
        });
        /** retrieve these from the server, this is easier than tree traversal */
      } else {
        const filterOptions = filtersStore.filterOptionsCache[filterName];
        if (filterOptions) {
          const queryValues = filtersToAdd.split(",");
          const activeFilters = filterOptions.filter((filterOption) =>
            queryValues.includes(filterOption.value)
          );
          filtersStore.updateFilter(filterName, activeFilters, true);
        }
      }
    }
  }
  filtersStore.bookmarkWaitingForApplication = false;
}

export function createBookmark(
  filters: Record<string, any>,
  collectionCart: Record<string, labelValuePair[]>,
  serviceCart: Record<string, labelValuePair[]>
) {
  const filtersStore = useFiltersStore();
  const bookmark: Record<string, string> = {};
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

        bookmark[filterName] = encodeURI(value);
      } else {
        let bookmarkValue = value;
        if (filterName === "Diagnosisavailable") {
          bookmarkValue = btoa(bookmarkValue);
        }

        bookmark[filterName] = encodeURI(value);
      }
    }
  }

  if (matchAll.length) {
    bookmark.matchAll = encodeURI(matchAll.join(","));
  }

  /** This manages the selection in the cart */
  if (collectionCart && Object.keys(collectionCart).length) {
    const bookmarkIds = [];
    for (const biobank in collectionCart) {
      bookmarkIds.push(
        collectionCart[biobank].map((collection) => collection.value)
      );
    }

    const encodedCart = btoa(bookmarkIds.join(","));
    bookmark.cart = encodeURI(encodedCart);
  }

  if (serviceCart && Object.keys(serviceCart).length) {
    const bookmarkIds = [];
    for (const service in serviceCart) {
      bookmarkIds.push(serviceCart[service].map((service) => service.value));
    }

    const encodedCart = btoa(bookmarkIds.join(","));
    bookmark.serviceCart = encodeURI(encodedCart);
  }

  if (!filtersStore.bookmarkWaitingForApplication) {
    try {
      router.push({
        name: router.currentRoute.value.name,
        query: bookmark,
      });
    } catch (error) {
      setError(error);
    }
  }
}

export default {
  createBookmark,
};
