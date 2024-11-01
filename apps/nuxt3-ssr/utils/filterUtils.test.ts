import { describe, it, expect } from "vitest";

import {
  isConditionFilter,
  toPathQueryConditions,
  conditionsFromPathQuery,
} from "./filterUtils";
import type {
  IFilter,
  IOntologyFilter,
  ISearchFilter,
} from "~/interfaces/types";

describe("isConditionFilter", () => {
  it("should return true if the filter is a condition filter", () => {
    const ontFilter: IOntologyFilter = {
      id: "areasOfInformation",
      config: {
        label: "Areas of information",
        type: "ONTOLOGY",
        ontologyTableId: "AreasOfInformationCohorts",
        ontologySchema: "CatalogueOntologies",
        columnId: "areasOfInformation",
        filterTable: "collectionEvents",
      },
      conditions: [],
    };
    expect(isConditionFilter(ontFilter)).toBe(true);
  });

  it("should return false if the filter is not a condition filter", () => {
    const searchFilter: ISearchFilter = {
      id: "search",
      config: {
        label: "Search in cohorts",
        type: "SEARCH",
        searchTables: ["collectionEvents", "subpopulations"],
        initialCollapsed: false,
      },
      search: "",
    };
    expect(isConditionFilter(searchFilter)).toBe(false);
  });
});

describe("toPathQueryConditions", () => {
  it("should return empty string if there are no filters set", () => {
    const filters: IFilter[] = [
      {
        id: "search",
        config: {
          label: "Search in cohorts",
          type: "SEARCH",
          searchTables: ["collectionEvents", "subpopulations"],
          initialCollapsed: false,
        },
        search: "",
      },
      {
        id: "areasOfInformation",
        config: {
          label: "Areas of information",
          type: "ONTOLOGY",
          ontologyTableId: "AreasOfInformationCohorts",
          ontologySchema: "CatalogueOntologies",
          columnId: "areasOfInformation",
          filterTable: "collectionEvents",
        },
        conditions: [],
      },
    ];
    const queryPath = toPathQueryConditions(filters);
    expect(queryPath).toBe("");
  });

  it("should transform filter state in to a conditions query string", () => {
    const filters: IFilter[] = [
      {
        id: "search",
        config: {
          label: "Search in cohorts",
          type: "SEARCH",
          searchTables: ["collectionEvents", "subpopulations"],
          initialCollapsed: false,
        },
        search: "foobar",
      },
      {
        id: "areasOfInformation",
        config: {
          label: "Areas of information",
          type: "ONTOLOGY",
          ontologyTableId: "AreasOfInformationCohorts",
          ontologySchema: "CatalogueOntologies",
          columnId: "areasOfInformation",
          filterTable: "collectionEvents",
        },
        conditions: [
          {
            name: "Adipocytes",
          },
          {
            name: "Myocytes, Cardiac",
          },
        ],
      },
    ];
    const queryPath = toPathQueryConditions(filters);
    expect(queryPath).toBe(
      '[{"id":"search","search":"foobar"},{"id":"areasOfInformation","conditions":[{"name":"Adipocytes"},{"name":"Myocytes, Cardiac"}]}]'
    );
  });

  describe("conditionsFromPathQuery", () => {
    it("should return empty array if there are no conditions", () => {
      const conditionsPathQuery = "";
      const conditions = conditionsFromPathQuery(conditionsPathQuery);
      expect(conditions).toEqual([]);
    });

    it("should return an array of conditions given a valid conditions query string", () => {
      const conditionsPathQuery =
        '[{"id":"search","search":"foobar"},{"id":"areasOfInformation","conditions":[{"name":"Adipocytes"},{"name":"Myocytes, Cardiac"}]}]';
      const conditions = conditionsFromPathQuery(conditionsPathQuery);
      expect(conditions).toEqual([
        { id: "search", search: "foobar" },
        {
          conditions: [{ name: "Adipocytes" }, { name: "Myocytes, Cardiac" }],
          id: "areasOfInformation",
        },
      ]);
    });
  });
});
