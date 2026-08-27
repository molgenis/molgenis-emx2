import { flushPromises, mount } from "@vue/test-utils";
import { readdirSync } from "fs";
import { resolve } from "path";
import { beforeEach, describe, expect, it, vi } from "vitest";
import DataList from "../../../../app/components/display/DataList.vue";
import type { IColumn, IRow } from "../../../../../metadata-utils/src/types";
import type { DisplayConfig } from "../../../../app/types/display";

const fetchTableDataMock = vi.fn();
const fetchTableMetadataMock = vi.fn();

vi.mock("../../../../app/composables/fetchTableData", () => ({
  default: (...args: unknown[]) => fetchTableDataMock(...args),
}));

vi.mock("../../../../app/composables/fetchTableMetadata", () => ({
  default: (...args: unknown[]) => fetchTableMetadataMock(...args),
}));

const nameColumn: IColumn = {
  id: "name",
  label: "Name",
  columnType: "STRING",
  key: 1,
};

const ageColumn: IColumn = {
  id: "age",
  label: "Age",
  columnType: "INT",
};

const refColumn: IColumn = {
  id: "owner",
  label: "Owner",
  columnType: "REF",
  refTableId: "Owner",
  refSchemaId: "test-schema",
  refLabelDefault: "${label}",
};

const columns: IColumn[] = [nameColumn, ageColumn];

function makeRows(count: number): IRow[] {
  return Array.from({ length: count }, (_, index) => ({
    name: `Bird ${index}`,
    age: index,
  }));
}

describe("DataList.vue", () => {
  beforeEach(() => {
    fetchTableDataMock.mockReset();
    fetchTableMetadataMock.mockReset();
  });

  describe("fixture mode (rows/columns passed in)", () => {
    it("renders TABLE layout by default, fetching nothing", () => {
      const wrapper = mount(DataList, {
        props: { rows: makeRows(2), columns },
      });

      expect(wrapper.find("table").exists()).toBe(true);
      expect(wrapper.findAll("tbody tr").length).toBe(2);
      expect(fetchTableDataMock).not.toHaveBeenCalled();
      expect(fetchTableMetadataMock).not.toHaveBeenCalled();
    });

    it.each([
      ["CARDS", "ul.grid-cols-2"],
      ["LIST", "ul.grid-cols-1"],
      ["LINKS", "ul.list-disc"],
    ] as const)("renders %s layout via display.layout", (layout, selector) => {
      const display: DisplayConfig = { layout };
      const wrapper = mount(DataList, {
        props: { rows: makeRows(2), columns, display },
      });

      expect(wrapper.find(selector).exists()).toBe(true);
    });

    it("resolves display once and passes detailColumns down to the layout", () => {
      const display: DisplayConfig = {
        layout: "TABLE",
        detailColumns: ["age"],
      };
      const wrapper = mount(DataList, {
        props: { rows: makeRows(1), columns, display },
      });

      const headers = wrapper.findAll("th").map((th) => th.text());
      expect(headers).toEqual(["Title", "Age"]);
    });

    it.each([
      ["rows", { rows: makeRows(9) }],
      ["pageSize", { pageSize: 4 }],
    ] as const)(
      "resets to page 1 when %s changes the result set",
      async (_label, propChange) => {
        const wrapper = mount(DataList, {
          props: { rows: makeRows(12), columns, pageSize: 5 },
        });
        const nextControl = wrapper.findAll("nav a").at(-1)!;
        await nextControl.trigger("click");
        expect(wrapper.findAll("tbody tr")[0].text()).toContain("Bird 5");

        await wrapper.setProps(propChange);

        expect(wrapper.findAll("tbody tr")[0].text()).toContain("Bird 0");
      }
    );
  });

  describe("fetch mode (schemaId/tableId passed in)", () => {
    beforeEach(() => {
      fetchTableMetadataMock.mockResolvedValue({
        id: "pet",
        columns,
      });
      fetchTableDataMock.mockResolvedValue({
        rows: makeRows(3),
        count: 3,
      });
    });

    it("calls fetchTableMetadata and fetchTableData with expandLevel 1, and renders the rows", async () => {
      const wrapper = mount(DataList, {
        props: { schemaId: "test-schema", tableId: "pet" },
      });
      await flushPromises();

      expect(fetchTableMetadataMock).toHaveBeenCalledWith("test-schema", "pet");
      expect(fetchTableDataMock).toHaveBeenCalledWith(
        "test-schema",
        "pet",
        expect.objectContaining({ expandLevel: 1 })
      );
      expect(wrapper.findAll("tbody tr").length).toBe(3);
    });

    it.each([
      ["CARDS", "ul.grid-cols-2"],
      ["LIST", "ul.grid-cols-1"],
      ["LINKS", "ul.list-disc"],
    ] as const)(
      "renders %s layout via display.layout in fetch mode",
      async (layout, selector) => {
        const display: DisplayConfig = { layout };
        const wrapper = mount(DataList, {
          props: { schemaId: "test-schema", tableId: "pet", display },
        });
        await flushPromises();

        expect(wrapper.find(selector).exists()).toBe(true);
      }
    );

    it("shows a ref cell as its label, not an object", async () => {
      fetchTableMetadataMock.mockResolvedValue({
        id: "pet",
        columns: [nameColumn, refColumn],
      });
      fetchTableDataMock.mockResolvedValue({
        rows: [{ name: "Tweety", owner: { label: "Morris" } }],
        count: 1,
      });

      const wrapper = mount(DataList, {
        props: {
          schemaId: "test-schema",
          tableId: "pet",
          display: { layout: "TABLE", detailColumns: ["owner"] },
        },
      });
      await flushPromises();

      expect(wrapper.find("tbody tr").text()).toContain("Morris");
    });

    it("re-queries fetchTableData when the page changes", async () => {
      fetchTableDataMock.mockResolvedValue({ rows: makeRows(3), count: 9 });
      const wrapper = mount(DataList, {
        props: { schemaId: "test-schema", tableId: "pet", pageSize: 3 },
      });
      await flushPromises();
      fetchTableDataMock.mockClear();

      const nextControl = wrapper.findAll("nav a").at(-1)!;
      await nextControl.trigger("click");
      await flushPromises();

      expect(fetchTableDataMock).toHaveBeenCalledWith(
        "test-schema",
        "pet",
        expect.objectContaining({ offset: 3, limit: 3 })
      );
    });

    it("re-queries with the new limit and resets to offset 0 when pageSize changes", async () => {
      fetchTableDataMock.mockResolvedValue({ rows: makeRows(3), count: 9 });
      const wrapper = mount(DataList, {
        props: { schemaId: "test-schema", tableId: "pet", pageSize: 3 },
      });
      await flushPromises();
      fetchTableDataMock.mockClear();

      await wrapper.setProps({ pageSize: 5 });
      await flushPromises();

      expect(fetchTableDataMock).toHaveBeenCalledWith(
        "test-schema",
        "pet",
        expect.objectContaining({ limit: 5, offset: 0 })
      );
    });

    it.each([
      [{ filter: { a: 2 } }, "pet"],
      [{ tableId: "other" }, "other"],
    ] as const)(
      "resets to offset 0 when %o changes after paging forward",
      async (propChange, expectedTableId) => {
        fetchTableDataMock.mockResolvedValue({ rows: makeRows(3), count: 9 });
        const wrapper = mount(DataList, {
          props: { schemaId: "test-schema", tableId: "pet", pageSize: 3 },
        });
        await flushPromises();
        const nextControl = wrapper.findAll("nav a").at(-1)!;
        await nextControl.trigger("click");
        await flushPromises();
        fetchTableDataMock.mockClear();

        await wrapper.setProps(propChange);
        await flushPromises();

        expect(fetchTableDataMock).toHaveBeenCalledWith(
          "test-schema",
          expectedTableId,
          expect.objectContaining({ offset: 0 })
        );
      }
    );

    it("discards a stale response when a newer request resolves later", async () => {
      let resolveFirst: (value: { rows: IRow[]; count: number }) => void;
      const firstResponse = new Promise<{ rows: IRow[]; count: number }>(
        (resolve) => {
          resolveFirst = resolve;
        }
      );
      fetchTableDataMock
        .mockReturnValueOnce(firstResponse)
        .mockResolvedValueOnce({
          rows: [{ name: "second", age: 1 }],
          count: 1,
        });

      const wrapper = mount(DataList, {
        props: { schemaId: "test-schema", tableId: "pet", pageSize: 3 },
      });
      await flushPromises();

      await wrapper.setProps({ filter: { a: 1 } });
      await flushPromises();

      resolveFirst!({ rows: [{ name: "first", age: 0 }], count: 1 });
      await flushPromises();

      expect(wrapper.find("tbody tr").text()).toContain("second");
    });
  });

  describe("paging", () => {
    it("renders only one page of rows in fixture mode and advances on next", async () => {
      const wrapper = mount(DataList, {
        props: { rows: makeRows(12), columns, pageSize: 5 },
      });

      expect(wrapper.findAll("tbody tr").length).toBe(5);

      const nextControl = wrapper.findAll("nav a").at(-1)!;
      await nextControl.trigger("click");

      expect(wrapper.findAll("tbody tr").length).toBe(5);
      expect(wrapper.findAll("tbody tr")[0].text()).toContain("Bird 5");
    });

    it("reuses Pagination with showPageSelector false: prev/next render, page-number box does not", () => {
      const wrapper = mount(DataList, {
        props: { rows: makeRows(12), columns, pageSize: 5 },
      });

      expect(wrapper.findAll("nav a").length).toBe(2);
      expect(wrapper.find("nav input").exists()).toBe(false);
    });

    it("renders the range and the total itself, including the empty and floor cases", () => {
      const wrapper = mount(DataList, {
        props: { rows: makeRows(12), columns, pageSize: 5 },
      });
      expect(wrapper.find("p").text()).toBe("1-5 of 12");

      const empty = mount(DataList, {
        props: { rows: [], columns, pageSize: 5 },
      });
      expect(empty.find("p").text()).toBe("0 of 0");
    });

    it("clamps the end of a last page that is not full", async () => {
      const wrapper = mount(DataList, {
        props: { rows: makeRows(57), columns, pageSize: 10 },
      });
      const nextControl = wrapper.findAll("nav a").at(-1)!;
      for (let click = 0; click < 5; click++) {
        await nextControl.trigger("click");
      }

      expect(wrapper.find("p").text()).toBe("51-57 of 57");
    });

    it("ships exactly the display/ components this story adds, alongside the untouched ones", () => {
      const files = readdirSync(
        resolve(__dirname, "../../../../app/components/display")
      ).sort();
      expect(files).toEqual([
        "CodeBlock.vue",
        "DataCards.vue",
        "DataLinks.vue",
        "DataList.vue",
        "DataTable.vue",
        "List.vue",
        "ListItem.vue",
        "Record.vue",
      ]);
    });
  });
});
