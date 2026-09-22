import {
  IFilterDetails,
  IFilterFacet,
  SortDirection,
} from "../interfaces/interfaces";
import {
  customFilterOptions,
  genericFilterOptions,
  ontologyFilterOptions,
} from "./filterOptions";

function getFacetIdentifier(facet: IFilterFacet): string {
  return facet.facetTitle
    ? facet.facetTitle.replaceAll(" ", "")
    : facet.applyToColumn || "";
}

export const filterTemplate = {
  facetTitle: "New Filter",
  component: "CheckboxFilter",
  sourceTable: "",
  applyToColumn: "",
  filterValueAttribute: "id",
  filterLabelAttribute: "name",
  removeOptions: [],
  customOptions:
    [] /** an array with objects that substitute calling the database for options, { text: labelToShow, value: valueToFilterOn } */,
  ontologyIdentifiers: [],
  trueOption: undefined /** use this in combination with a toggle filter */,
  showMatchTypeSelector: true,
  negotiatorRequestString: "New Filter:",
  adaptive: false,
  sortColumn: "name",
  sortDirection: "asc",
  showFacet: true,
};

export function createFilters(
  filterFacets: IFilterFacet[]
): Record<string, IFilterDetails> {
  const filterDetails: Record<string, IFilterDetails> = {};

  for (const facet of filterFacets) {
    const facetId = getFacetIdentifier(facet);
    filterDetails[facetId] = {
      facetTitle:
        facet.facetTitle ||
        facet.applyToColumn ||
        "invalidFacetTitle" /** a custom 'human readable' text for on the button */,
      facetIdentifier: facetId,
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
      options: getFilterOptions({
        ...facet,
        facetIdentifier: facetId,
      }),
      ontologyIdentifiers:
        facet.ontologyIdentifiers ||
        [] /** for use when you have multiple ontologies in a single table, e.g. orhpa and icd */,
      trueOption: facet.trueOption /** use this for a togglefilter */,
      matchTypeForFilter:
        "any" /** if it has been selected from bookmark, it will be applied here. */,
      showMatchTypeSelector: facet.showMatchTypeSelector ?? true,
      negotiatorRequestString: facet.negotiatorRequestString,
      builtIn:
        facet.builtIn ||
        false /** if this filter should be ignored for dropdown filters generation */,
      showFacet: facet.showFacet || false,
      adaptive:
        facet.adaptive ||
        false /** if the filters options should react on search results */,
      sortColumn:
        facet.sortColumn || "name" /** specify a column to apply sorting on */,
      sortDirection:
        (facet.sortDirection?.toLowerCase() as SortDirection) || "asc",
    };
  }
  return filterDetails;
}

function getFilterOptions(filterFacet: IFilterFacet) {
  if (filterFacet.customOptions && filterFacet.customOptions.length > 0) {
    return customFilterOptions(filterFacet);
  } else if (filterFacet.component === "OntologyFilter") {
    return ontologyFilterOptions(filterFacet);
  } else {
    return genericFilterOptions(filterFacet);
  }
}
