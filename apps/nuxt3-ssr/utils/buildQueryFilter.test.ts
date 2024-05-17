import { describe, it, expect } from "vitest";

import { buildQueryFilter } from "./buildQueryFilter";
import type { IFilter, IFilterCondition } from "~/interfaces/types";

describe("buildQueryFilter", () => {
  const filters: IFilter[] = [
    {
      id: "search",
      config: {
        label: "Search in cohorts",
        type: "SEARCH",
        searchTables: ["collectionEvents", "subcohorts"],
        initialCollapsed: false,
      },
      search: "test",
    },
    {
      id: "sampleCategories",
      config: {
        label: "Sample categories",
        columnId: "sampleCategories",
        type: "ONTOLOGY",
        ontologyTableId: "SampleCategories",
        ontologySchema: "public",
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

  it("should add the search command to each of the filter tables combining them using a OR ", () => {
    const expectedFilter = {
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
    };
    const filterString = buildQueryFilter(filters);
    expect(expectedFilter).toEqual(filterString);
  });

  it("should use the buildFilterFunction to build the filter confition(s) if it is set on the filter config", () => {
    const filtersWithFunction: IFilter[] = [
      {
        id: "cohorts",
        config: {
          label: "Cohorts",
          type: "REF_ARRAY",
          refTableId: "Cohorts",
          buildFilterFunction: (_: any, conditions: IFilterCondition[]) => {
            return {
              mappings: {
                source: { equals: conditions.map((c) => ({ id: c.name })) },
              },
            };
          },
          refFields: {
            key: "id",
            name: "id",
            description: "name",
          },
        },
        conditions: [
          {
            name: "foo",
          },
          {
            name: "bar",
          },
        ],
      },
    ];
    expect(buildQueryFilter(filtersWithFunction)).toEqual({
      mappings: {
        source: {
          equals: [
            {
              id: "foo",
            },
            {
              id: "bar",
            },
          ],
        },
      },
    });
  });
});
