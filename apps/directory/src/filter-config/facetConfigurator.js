import {
  customFilterOptions,
  genericFilterOptions,
  ontologyFilterOptions,
} from "./filterOptions";
import { useSettingsStore } from "../stores/settingsStore";

export const filterTemplate = {
  facetTitle: "New Filter",
  component: "CheckboxFilter",
  sourceTable: "",
  applyToColumn: "",
  filterValueAttribute: "id",
  filterLabelAttribute: "name",
  removeOptions: [],
  customOptions: [] /** an array with objects that substitute calling the database for options, { text: labelToShow, value: valueToFilterOn } */,
  showMatchTypeSelector: true,
  negotiatorRequestString: "New Filter:",
  adaptive: false,
  sortColumn: "name",
  sortDirection: "asc",
  showFacet: true,
};

export function createFilters(filters) {
  const settingsStore = useSettingsStore();

  const filterFacets = [];

  for (const facet of settingsStore.config.filterFacets) {
    filterFacets.push({
      facetTitle:
        facet.facetTitle ||
        facet.applyToColumn /** a custom 'human readable' text for on the button */,
      facetIdentifier: facet.facetTitle
        ? facet.facetTitle.replaceAll(" ", "")
        : facet.applyToColumn,
      component:
        facet.component ||
        "CheckboxFilter" /** a custom specified component, or just the default */,
      sourceTable:
        facet.sourceTable /** the table where the options are coming from. */,
      applyToColumn:
        facet.applyToColumn /** the column in the main table to apply the filter on. */,
      filterValueAttribute:
        facet.filterValueAttribute ||
        "id" /** specify a column name if you want a different column for the value */,
      filterLabelAttribute:
        facet.filterLabelAttribute ||
        "name" /** specify if you want to use another column as the label for the filter option, instead of name */,
      options: getFilterOptions(
        facet
      ) /** uses the removeOptions array provided in the configuration */,
      ontologyIdentifiers: facet.ontologyIdentifiers || [],
      filters:
        filters[facet.name] || [] /** adds the currently active options */,
      matchTypeForFilter:
        "any" /** if it has been selected from bookmark, it will be applied here. */,
      showMatchTypeSelector:
        facet.showMatchTypeSelector ||
        true /** if you want to make match all / match any available */,
      negotiatorRequestString:
        facet.negotiatorRequestString /** the part that will be send to the negotiator as to indicate what it is */,
      builtIn:
        facet.builtIn /** if this filter should be ignored for dropdown filters generation */,
      showFacet:
        facet.showFacet || true /** if this filter should be shown at all */,
      adaptive:
        facet.adaptive ||
        false /** if the filters options should react on search results */,
      sortColumn: "name" /** specify a column to apply sorting on */,
      sortDirection: "asc" /** the direction to sort */,
    });
  }
  return filterFacets;
}

function getFilterOptions(filterFacet) {
  if (filterFacet.customOptions && filterFacet.customOptions.length > 0) {
    return customFilterOptions(filterFacet);
  } else if (filterFacet.component === "OntologyFilter") {
    return ontologyFilterOptions(filterFacet);
  } else {
    return genericFilterOptions(filterFacet);
  }
}
