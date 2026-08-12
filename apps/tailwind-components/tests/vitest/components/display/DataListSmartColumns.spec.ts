import { flushPromises, mount } from "@vue/test-utils";
import { afterEach, describe, expect, it, vi } from "vitest";
import DataList from "../../../../app/components/display/DataList.vue";
import type { IColumn } from "../../../../../metadata-utils/src/types";

function col(overrides: Partial<IColumn>): IColumn {
  return {
    id: "test",
    label: "Test",
    columnType: "STRING",
    position: 0,
    ...overrides,
  } as IColumn;
}

const COLLECTION_COLUMNS = [
  col({ id: "id", label: "Id", key: 1 }),
  col({ id: "acronym", label: "Acronym", role: "TITLE" }),
  col({ id: "name", label: "Name", role: "SUBTITLE" }),
  col({ id: "startYear", label: "Start year", columnType: "INT" }),
];

const COLLECTION_ROWS = [
  { id: "c1", acronym: "AVO", name: "Avohilmo", startYear: 2011 },
  { id: "c2", acronym: "LSC", name: "Lifelines", startYear: 2006 },
];

function stubTableGraphql(schemaId: string, tableId: string) {
  vi.stubGlobal(
    "$fetch",
    vi.fn(async (_url: string, options: { body: { query: string } }) => {
      if (options.body.query.includes("_schema")) {
        return {
          data: {
            _schema: {
              id: schemaId,
              tables: [{ id: tableId, columns: COLLECTION_COLUMNS }],
            },
          },
        };
      }
      return {
        data: {
          [tableId]: COLLECTION_ROWS,
          [`${tableId}_agg`]: { count: COLLECTION_ROWS.length },
        },
      };
    })
  );
}

async function mountSmartList(
  schemaId: string,
  tableId: string,
  extraProps: Record<string, unknown> = {}
) {
  const wrapper = mount(DataList, {
    props: {
      schemaId,
      tableId,
      layout: "TABLE",
      ...extraProps,
    },
  });
  for (let attempt = 0; attempt < 6; attempt++) await flushPromises();
  return wrapper;
}

function headerLabels(wrapper: ReturnType<typeof mount>): string[] {
  return wrapper.findAll("th").map((th) => th.text());
}

describe("DataList — smart mode column selection", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("keeps the existing role-capped selection when no visibleColumns are given", async () => {
    stubTableGraphql("AllColumnsTableSchema", "AllColumnsTable");

    const wrapper = await mountSmartList(
      "AllColumnsTableSchema",
      "AllColumnsTable"
    );

    expect(headerLabels(wrapper)).toEqual(["Acronym", "Start year"]);
  });

  it("restricts smart-mode columns to visibleColumns, in the order given", async () => {
    stubTableGraphql("VisibleColumnsTableSchema", "VisibleColumnsTable");

    const wrapper = await mountSmartList(
      "VisibleColumnsTableSchema",
      "VisibleColumnsTable",
      {
        visibleColumns: ["acronym", "name"],
      }
    );

    expect(headerLabels(wrapper)).toEqual(["Acronym", "Name"]);
  });
});

describe("DataList — search ownership", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("shows its own search box in smart mode by default", async () => {
    stubTableGraphql("OwnSearchTableSchema", "OwnSearchTable");

    const wrapper = await mountSmartList(
      "OwnSearchTableSchema",
      "OwnSearchTable",
      { pageSize: 1 }
    );

    expect(wrapper.find("input[type='search']").exists()).toBe(true);
  });

  it("hides its own search box when the parent owns search", async () => {
    stubTableGraphql("ParentSearchTableSchema", "ParentSearchTable");

    const wrapper = await mountSmartList(
      "ParentSearchTableSchema",
      "ParentSearchTable",
      {
        pageSize: 1,
        hideSearch: true,
      }
    );

    expect(wrapper.find("input[type='search']").exists()).toBe(false);
  });
});
