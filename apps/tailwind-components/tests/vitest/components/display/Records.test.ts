import { flushPromises, mount } from "@vue/test-utils";
import { readdirSync } from "fs";
import { resolve } from "path";
import { beforeEach, describe, expect, it, vi } from "vitest";
import Records from "../../../../app/components/display/Records.vue";
import Pagination from "../../../../app/components/Pagination.vue";
import ShowMore from "../../../../app/components/ShowMore.vue";
import RecordsCards from "../../../../app/components/display/records/Cards.vue";
import RecordsList from "../../../../app/components/display/records/List.vue";
import RecordsLinks from "../../../../app/components/display/records/Links.vue";
import RecordsBullets from "../../../../app/components/display/records/Bullets.vue";
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

describe("Records.vue", () => {
  beforeEach(() => {
    fetchTableDataMock.mockReset();
    fetchTableMetadataMock.mockReset();
    fetchTableMetadataMock.mockResolvedValue({ id: "pet", columns });
    fetchTableDataMock.mockResolvedValue({ rows: makeRows(3), count: 3 });
  });

  describe("layout dispatch", () => {
    it("calls fetchTableMetadata and fetchTableData with expandLevel 1, and renders TABLE by default", async () => {
      const wrapper = mount(Records, {
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

    it("renders CARDS layout via displayConfig.layout", async () => {
      const displayConfig: DisplayConfig = { layout: "CARDS" };
      const wrapper = mount(Records, {
        props: { schemaId: "test-schema", tableId: "pet", displayConfig },
      });
      await flushPromises();

      expect(wrapper.findComponent(RecordsCards).exists()).toBe(true);
    });

    it("renders LIST layout via displayConfig.layout, not RecordsCards", async () => {
      const displayConfig: DisplayConfig = { layout: "LIST" };
      const wrapper = mount(Records, {
        props: { schemaId: "test-schema", tableId: "pet", displayConfig },
      });
      await flushPromises();

      expect(wrapper.findComponent(RecordsList).exists()).toBe(true);
      expect(wrapper.findComponent(RecordsCards).exists()).toBe(false);
    });

    it("renders LINKS layout via displayConfig.layout", async () => {
      const displayConfig: DisplayConfig = { layout: "LINKS" };
      const wrapper = mount(Records, {
        props: { schemaId: "test-schema", tableId: "pet", displayConfig },
      });
      await flushPromises();

      expect(wrapper.findComponent(RecordsLinks).exists()).toBe(true);
    });

    it("renders BULLETS layout via displayConfig.layout, not RecordsLinks", async () => {
      const displayConfig: DisplayConfig = { layout: "BULLETS" };
      const wrapper = mount(Records, {
        props: { schemaId: "test-schema", tableId: "pet", displayConfig },
      });
      await flushPromises();

      expect(wrapper.findComponent(RecordsBullets).exists()).toBe(true);
      expect(wrapper.findComponent(RecordsLinks).exists()).toBe(false);
    });

    it("resolves displayConfig once and passes detailColumns down to the layout", async () => {
      const displayConfig: DisplayConfig = {
        layout: "TABLE",
        detailColumnIds: ["age"],
      };
      const wrapper = mount(Records, {
        props: { schemaId: "test-schema", tableId: "pet", displayConfig },
      });
      await flushPromises();

      const headers = wrapper.findAll("th").map((th) => th.text());
      expect(headers).toEqual(["Title", "Age"]);
    });

    it("forwards hideEmpty to the layout it renders", async () => {
      const listDisplayConfig: DisplayConfig = { layout: "LIST" };
      const wrapper = mount(Records, {
        props: {
          schemaId: "test-schema",
          tableId: "pet",
          displayConfig: listDisplayConfig,
          hideEmpty: false,
        },
      });
      await flushPromises();

      const layout = wrapper.findComponent(RecordsList);
      expect(layout.props("hideEmpty")).toBe(false);
    });

    it("shows a ref cell as its label, not an object", async () => {
      fetchTableMetadataMock.mockResolvedValue({
        id: "pet",
        columns: [nameColumn, refColumn],
      });
      fetchTableDataMock.mockResolvedValue({
        rows: [{ name: "Tweety", owner: { label: "Morris" } }],
        count: 1,
      });

      const wrapper = mount(Records, {
        props: {
          schemaId: "test-schema",
          tableId: "pet",
          displayConfig: { layout: "TABLE", detailColumnIds: ["owner"] },
        },
      });
      await flushPromises();

      expect(wrapper.find("tbody tr").text()).toContain("Morris");
    });
  });

  describe("paging", () => {
    it("re-queries fetchTableData when the page changes", async () => {
      fetchTableDataMock.mockResolvedValue({ rows: makeRows(3), count: 9 });
      const wrapper = mount(Records, {
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
      const wrapper = mount(Records, {
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

    it("resets to page 1 when filter changes the result set", async () => {
      fetchTableDataMock.mockResolvedValue({ rows: makeRows(3), count: 9 });
      const wrapper = mount(Records, {
        props: { schemaId: "test-schema", tableId: "pet", pageSize: 3 },
      });
      await flushPromises();
      const nextControl = wrapper.findAll("nav a").at(-1)!;
      await nextControl.trigger("click");
      await flushPromises();
      fetchTableDataMock.mockClear();

      await wrapper.setProps({ filter: { a: 2 } });
      await flushPromises();

      expect(fetchTableDataMock).toHaveBeenCalledWith(
        "test-schema",
        "pet",
        expect.objectContaining({ offset: 0 })
      );
    });

    it("resets to page 1 when tableId changes the result set", async () => {
      fetchTableDataMock.mockResolvedValue({ rows: makeRows(3), count: 9 });
      const wrapper = mount(Records, {
        props: { schemaId: "test-schema", tableId: "pet", pageSize: 3 },
      });
      await flushPromises();
      const nextControl = wrapper.findAll("nav a").at(-1)!;
      await nextControl.trigger("click");
      await flushPromises();
      fetchTableDataMock.mockClear();

      await wrapper.setProps({ tableId: "other" });
      await flushPromises();

      expect(fetchTableDataMock).toHaveBeenCalledWith(
        "test-schema",
        "other",
        expect.objectContaining({ offset: 0 })
      );
    });

    it("discards a stale metadata response so headers don't flip back after tableId changes", async () => {
      const weightColumn: IColumn = {
        id: "weight",
        label: "Weight",
        columnType: "INT",
      };
      let resolvePetMetadata: (value: {
        id: string;
        columns: IColumn[];
      }) => void;
      const petMetadata = new Promise<{ id: string; columns: IColumn[] }>(
        (resolve) => {
          resolvePetMetadata = resolve;
        }
      );
      fetchTableMetadataMock
        .mockReturnValueOnce(petMetadata)
        .mockResolvedValueOnce({ id: "other", columns: [weightColumn] });
      fetchTableDataMock.mockResolvedValue({ rows: makeRows(1), count: 1 });

      const wrapper = mount(Records, {
        props: { schemaId: "test-schema", tableId: "pet" },
      });
      await flushPromises();

      await wrapper.setProps({ tableId: "other" });
      await flushPromises();

      resolvePetMetadata!({ id: "pet", columns });
      await flushPromises();

      const headers = wrapper.findAll("th").map((th) => th.text());
      expect(headers).toEqual(["Title", "Weight"]);
    });

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

      const wrapper = mount(Records, {
        props: { schemaId: "test-schema", tableId: "pet", pageSize: 3 },
      });
      await flushPromises();

      await wrapper.setProps({ filter: { a: 1 } });
      await flushPromises();

      resolveFirst!({ rows: [{ name: "first", age: 0 }], count: 1 });
      await flushPromises();

      expect(wrapper.find("tbody tr").text()).toContain("second");
    });

    it("renders the range and the total itself, including the empty and floor cases", async () => {
      const wrapper = mount(Records, {
        props: { schemaId: "test-schema", tableId: "pet", pageSize: 3 },
      });
      await flushPromises();
      expect(wrapper.find("p").text()).toBe("1 - 3 of 3");

      fetchTableDataMock.mockResolvedValueOnce({ rows: [], count: 0 });
      const empty = mount(Records, {
        props: { schemaId: "test-schema", tableId: "pet", pageSize: 3 },
      });
      await flushPromises();
      expect(empty.find("p").text()).toBe("0 of 0");
    });

    it("clamps the end of a last page that is not full, after the next control advances", async () => {
      fetchTableDataMock.mockResolvedValue({ rows: makeRows(10), count: 57 });
      const wrapper = mount(Records, {
        props: { schemaId: "test-schema", tableId: "pet", pageSize: 10 },
      });
      await flushPromises();

      fetchTableDataMock.mockResolvedValue({ rows: makeRows(7), count: 57 });
      const nextControl = wrapper.findAll("nav a").at(-1)!;
      for (let click = 0; click < 5; click++) {
        await nextControl.trigger("click");
        await flushPromises();
      }

      expect(wrapper.find("p").text()).toBe("51 - 57 of 57");
    });

    it("adds no file under display/ named like a pagination component", () => {
      const files = readdirSync(
        resolve(__dirname, "../../../../app/components/display")
      );
      const pagerFiles = files.filter((file) =>
        /pag(e|er|ination)/i.test(file)
      );
      expect(pagerFiles).toEqual([]);
    });

    it.each([
      ["TABLE", true],
      ["CARDS", true],
      ["LIST", true],
      ["LINKS", false],
      ["BULLETS", false],
    ] as const)(
      "renders Pagination for %s only when the layout pages (paginated: %s)",
      async (layout, paginated) => {
        const displayConfig: DisplayConfig = { layout };
        const wrapper = mount(Records, {
          props: { schemaId: "test-schema", tableId: "pet", displayConfig },
        });
        await flushPromises();

        expect(wrapper.findComponent(Pagination).exists()).toBe(paginated);
      }
    );
  });

  describe("load more (LINKS/BULLETS)", () => {
    it.each([
      ["LINKS", 50],
      ["BULLETS", 20],
    ] as const)(
      "requests %s's own batch size on the first fetch, not pageSize",
      async (layout, batchSize) => {
        const displayConfig: DisplayConfig = { layout };
        mount(Records, {
          props: {
            schemaId: "test-schema",
            tableId: "pet",
            pageSize: 3,
            displayConfig,
          },
        });
        await flushPromises();

        expect(fetchTableDataMock).toHaveBeenCalledWith(
          "test-schema",
          "pet",
          expect.objectContaining({ limit: batchSize, offset: 0 })
        );
      }
    );

    it("shows remaining in the load-more control, advances offset on click, and drops the control at zero", async () => {
      const displayConfig: DisplayConfig = { layout: "LINKS" };
      fetchTableDataMock
        .mockResolvedValueOnce({ rows: makeRows(50), count: 130 })
        .mockResolvedValueOnce({ rows: makeRows(50), count: 130 })
        .mockResolvedValueOnce({ rows: makeRows(30), count: 130 });

      const wrapper = mount(Records, {
        props: { schemaId: "test-schema", tableId: "pet", displayConfig },
      });
      await flushPromises();

      const control = () => wrapper.findComponent(ShowMore).find("button");
      expect(control().text()).toBe("Load more (80)");

      await control().trigger("click");
      await flushPromises();

      expect(fetchTableDataMock).toHaveBeenLastCalledWith(
        "test-schema",
        "pet",
        expect.objectContaining({ offset: 50, limit: 50 })
      );
      expect(control().text()).toBe("Load more (30)");

      await control().trigger("click");
      await flushPromises();

      expect(fetchTableDataMock).toHaveBeenLastCalledWith(
        "test-schema",
        "pet",
        expect.objectContaining({ offset: 100, limit: 50 })
      );
      expect(wrapper.findComponent(ShowMore).find("button").exists()).toBe(
        false
      );
    });

    it("discards a stale load-more append when a filter change lands first", async () => {
      let resolveLoadMore: (value: { rows: IRow[]; count: number }) => void;
      const loadMoreResponse = new Promise<{ rows: IRow[]; count: number }>(
        (resolve) => {
          resolveLoadMore = resolve;
        }
      );
      fetchTableDataMock
        .mockResolvedValueOnce({ rows: makeRows(50), count: 130 })
        .mockReturnValueOnce(loadMoreResponse)
        .mockResolvedValueOnce({ rows: makeRows(1), count: 1 });

      const displayConfig: DisplayConfig = { layout: "LINKS" };
      const wrapper = mount(Records, {
        props: { schemaId: "test-schema", tableId: "pet", displayConfig },
      });
      await flushPromises();

      await wrapper.findComponent(ShowMore).find("button").trigger("click");
      await wrapper.setProps({ filter: { a: 1 } });
      await flushPromises();

      resolveLoadMore!({ rows: makeRows(50), count: 130 });
      await flushPromises();

      expect(wrapper.findComponent(RecordsLinks).props("rows")).toHaveLength(1);
    });

    it("renders no load-more control and no range line for LINKS while the fetch is still pending", () => {
      fetchTableDataMock.mockReturnValue(new Promise(() => {}));
      const displayConfig: DisplayConfig = { layout: "LINKS" };
      const wrapper = mount(Records, {
        props: { schemaId: "test-schema", tableId: "pet", displayConfig },
      });

      expect(wrapper.findComponent(ShowMore).props("hasMore")).toBe(false);
      expect(wrapper.find("p.text-pagination").exists()).toBe(false);
    });
  });
});
