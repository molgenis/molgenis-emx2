import { describe, it, expect } from "vitest";

import { buildQueryFilter } from "./buildQueryFilter";
import type { IFilter } from "~/interfaces/types";

describe("buildQueryFilter", () => {
  let filters: IFilter[] = [
    {
      title: "Search in cohorts",
      columnType: "_SEARCH",
      search: "",
      searchTables: ["collectionEvents", "subcohorts"],
      initialCollapsed: false,
    },
    {
      title: "Sample categories",
      refTableId: "SampleCategories",
      columnId: "sampleCategories",
      columnType: "ONTOLOGY",
      filterTable: "collectionEvents",
      conditions: [{ name: "Adipocytes" }, { name: "Myocytes, Cardiac" }],
    },
  ];

  it("should add the search command to each of the filter tables combining them using a OR ", () => {
    const expectedFilter = {
      _and: {
        _or: [
          {
            _search: "test",
          },
          {
            collectionEvents: {
              _search: "test",
            },
          },
          {
            subcohorts: {
              _search: "test",
            },
          },
        ],
        collectionEvents: {
          sampleCategories: {
            equals: [
              {
                name: "Adipocytes",
              },
              {
                name: "Myocytes, Cardiac",
              },
            ],
          },
        },
      },
    };
    const filterString = buildQueryFilter(filters, "test");
    expect(expectedFilter).toEqual(filterString);
  });
});
