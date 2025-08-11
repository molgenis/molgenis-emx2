import { LocationQuery } from "vue-router";
import useErrorHandler from "../composables/errorHandler";
import { IOntologyItem } from "../interfaces/interfaces";
import router from "../router";
import { ILabelValuePair, useCheckoutStore } from "../stores/checkoutStore";
import { useCollectionStore } from "../stores/collectionStore";
import { useFiltersStore } from "../stores/filtersStore";
import * as _ from "lodash";

let bookmarkApplied = false;

const { setError, clearError } = useErrorHandler();

export async function applyBookmark(watchedQuery: LocationQuery) {
  if (bookmarkApplied) {
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
  bookmarkApplied = true;
}

export function createBookmark(
  filters: Record<string, any>,
  collectionCart: Record<string, ILabelValuePair[]>,
  serviceCart: Record<string, ILabelValuePair[]>,
  filterTypes: Record<string, any>,
  bookmarkWaitingForApplication: boolean
) {
  const bookmarkQuery: Record<string, string> = {};
  const matchAll = [];

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

    const filterType = filterTypes[filterName] || "any";

    if (filterType === "all") {
      matchAll.push(filterName);
    }

    if (Array.isArray(value) && value.length) {
      bookmarkQuery[filterName] = createBookmarkForArray(value, filterName);
    } else if (typeof value === "object") {
      bookmarkQuery[filterName] = createBookmarkForObject(value, filterName);
    } else {
      bookmarkQuery[filterName] = createBookmarkForOther(value, filterName);
    }
  }

  if (matchAll.length) {
    bookmarkQuery.matchAll = encodeURI(matchAll.join(","));
  }

  if (collectionCart && Object.keys(collectionCart).length) {
    bookmarkQuery.cart = createBookmarkForCart(collectionCart);
  }

  if (serviceCart && Object.keys(serviceCart).length) {
    const bookmarkIds = [];
    for (const service in serviceCart) {
      bookmarkIds.push(serviceCart[service].map((service) => service.value));
    }

    const encodedCart = btoa(bookmarkIds.join(","));
    bookmarkQuery.serviceCart = encodeURI(encodedCart);
  }

  if (!bookmarkWaitingForApplication) {
    try {
      clearError();
      router.push({
        name: router.currentRoute.value.name,
        query: bookmarkQuery,
      });
    } catch (error) {
      setError(error);
    }
  }
}

function createBookmarkForArray(value: any[], filterName: string) {
  const extractedValues = value.map(
    /** ontology / checkbox / other */
    (val) => val["code"] || val["value"] || val["name"]
  );
  const bookmarkValue = extractedValues.join(",");
  return filterName === "Diagnosisavailable"
    ? encodeURI(btoa(bookmarkValue))
    : encodeURI(bookmarkValue);
}

function createBookmarkForObject(
  value: Record<string, any>,
  filterName: string
) {
  const bookmarkValue = value["code"] || value["value"] || value["name"];
  return filterName === "Diagnosisavailable"
    ? encodeURI(btoa(bookmarkValue))
    : encodeURI(bookmarkValue);
}

function createBookmarkForOther(value: any, filterName: string) {
  return filterName === "Diagnosisavailable"
    ? encodeURI(btoa(value))
    : encodeURI(value);
}

function createBookmarkForCart(
  collectionCart: Record<string, ILabelValuePair[]>
) {
  /** This manages the selection in the cart */
  const bookmarkIds = [];
  for (const biobankId in collectionCart) {
    bookmarkIds.push(
      collectionCart[biobankId].map((collection) => collection.value)
    );
  }

  const encodedCart = btoa(bookmarkIds.join(","));
  return encodeURI(encodedCart);
}
