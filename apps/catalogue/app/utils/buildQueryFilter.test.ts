import { describe, it, expect } from "vitest";

import { buildQueryFilter } from "./buildQueryFilter";
import type { IFilter, IFilterCondition } from "../../interfaces/types";

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

  it("should match both Resources.areasOfInformation and collectionEvents.areasOfInformation", () => {
    const areasFilter: IFilter[] = [
      {
        id: "areasOfInformation",
        config: {
          label: "Areas of information",
          type: "ONTOLOGY",
          ontologyTableId: "AreasOfInformationCohorts",
          ontologySchema: "CatalogueOntologies",
          columnId: "areasOfInformation",
          buildFilterFunction: (
            filterBuilder: Record<string, Record<string, any>>,
            conditions: IFilterCondition[]
          ) => ({
            ...filterBuilder,
            _and: [
              ...(Array.isArray(filterBuilder._and)
                ? filterBuilder._and
                : []),
              {
                _or: [
                  { areasOfInformation: { equals: conditions } },
                  {
                    collectionEvents: {
                      areasOfInformation: { equals: conditions },
                    },
                  },
                ],
              },
            ],
          }),
        },
        conditions: [{ name: "Tobacco" }],
      },
    ];

    expect(buildQueryFilter(areasFilter)).toEqual({
      _and: [
        {
          _or: [
            { areasOfInformation: { equals: [{ name: "Tobacco" }] } },
            {
              collectionEvents: {
                areasOfInformation: { equals: [{ name: "Tobacco" }] },
              },
            },
          ],
        },
      ],
    });
  });

  it("should compose areasOfInformation filter with a SEARCH filter without clobbering", () => {
    const combined: IFilter[] = [
      {
        id: "search",
        config: {
          label: "Search in collections",
          type: "SEARCH",
          searchTables: ["collectionEvents", "subpopulations"],
        },
        search: "cancer",
      },
      {
        id: "areasOfInformation",
        config: {
          label: "Areas of information",
          type: "ONTOLOGY",
          ontologyTableId: "AreasOfInformationCohorts",
          ontologySchema: "CatalogueOntologies",
          columnId: "areasOfInformation",
          buildFilterFunction: (
            filterBuilder: Record<string, Record<string, any>>,
            conditions: IFilterCondition[]
          ) => ({
            ...filterBuilder,
            _and: [
              ...(Array.isArray(filterBuilder._and)
                ? filterBuilder._and
                : []),
              {
                _or: [
                  { areasOfInformation: { equals: conditions } },
                  {
                    collectionEvents: {
                      areasOfInformation: { equals: conditions },
                    },
                  },
                ],
              },
            ],
          }),
        },
        conditions: [{ name: "Tobacco" }],
      },
    ];

    const result: any = buildQueryFilter(combined);
    expect(result._or).toBeDefined();
    expect(result._and).toBeDefined();
    expect(JSON.stringify(result._and)).toContain("Tobacco");
    expect(
      JSON.stringify(result._and).replace(
        /"collectionEvents":\s*\{[^}]*\}/,
        ""
      )
    ).toContain("areasOfInformation");
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
