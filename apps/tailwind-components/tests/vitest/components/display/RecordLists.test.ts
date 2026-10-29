import { enableAutoUnmount, flushPromises, mount } from "@vue/test-utils";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { clearNuxtData } from "#app";
import type { IColumn, IRow } from "../../../../../metadata-utils/src/types";
import DisplayRecord from "../../../../app/components/display/Record.vue";
import DisplayRecords from "../../../../app/components/display/Records.vue";
import RecordsTable from "../../../../app/components/display/records/Table.vue";
import RecordsCards from "../../../../app/components/display/records/Cards.vue";
import RecordsList from "../../../../app/components/display/records/List.vue";
import RecordsLinks from "../../../../app/components/display/records/Links.vue";
import RecordsBullets from "../../../../app/components/display/records/Bullets.vue";
import type { DisplayConfig, Layout } from "../../../../app/types/display";

enableAutoUnmount(afterEach);

const fetchTableDataMock = vi.fn();
const fetchTableMetadataMock = vi.fn();

vi.mock("../../../../app/composables/fetchTableData", () => ({
  default: (...args: unknown[]) => fetchTableDataMock(...args),
}));

vi.mock("../../../../app/composables/fetchTableMetadata", () => ({
  default: (...args: unknown[]) => fetchTableMetadataMock(...args),
}));

const petColumns: IColumn[] = [
  { id: "name", label: "Name", columnType: "STRING", key: 1 },
];

const nameColumn: IColumn = {
  id: "name",
  label: "Name",
  columnType: "STRING",
  key: 1,
};
const speciesColumn: IColumn = {
  id: "species",
  label: "Species",
  columnType: "STRING",
};
const petsColumn: IColumn = {
  id: "pets",
  label: "Pets",
  columnType: "REFBACK",
  refTableId: "Pet",
  refSchemaId: "test-schema",
  refBackId: "owner",
};

const row: IRow = {
  name: "Alice",
  species: "human",
  pets: [{ name: "Bird 0" }, { name: "Bird 1" }],
  pets_agg: { count: 2 },
};

const layouts = {
  TABLE: RecordsTable,
  CARDS: RecordsCards,
  LIST: RecordsList,
  LINKS: RecordsLinks,
  BULLETS: RecordsBullets,
} as const;

function mountRecord(
  columns: IColumn[],
  recordRow: IRow,
  layout: Layout | null = "LIST",
  displayConfig?: DisplayConfig
) {
  return mount(DisplayRecord, {
    props: {
      columns,
      row: recordRow,
      showTableOfContents: false,
      lists: layout ? { pets: displayConfig ?? { layout } } : undefined,
    },
  });
}

describe("DisplayRecord nested lists", () => {
  beforeEach(() => {
    clearNuxtData();
    fetchTableDataMock.mockReset();
    fetchTableMetadataMock.mockReset();
    fetchTableMetadataMock.mockResolvedValue({
      id: "Pet",
      columns: petColumns,
    });
    fetchTableDataMock.mockResolvedValue({
      rows: [{ name: "Bird 0" }, { name: "Bird 1" }],
      count: 2,
    });
  });

  it.each(Object.entries(layouts))(
    "renders a %s list under a small label, after the field that precedes it",
    async (layout, component) => {
      const wrapper = mountRecord(
        [nameColumn, speciesColumn, petsColumn],
        row,
        layout as Layout
      );
      await flushPromises();

      const section = wrapper.get("section");
      const list = section.findComponent(component);
      expect(list.exists()).toBe(true);
      expect(list.text()).toContain("Bird 0");

      const definitionList = section.get("dl");
      const heading = section.get("h4");
      expect(heading.text()).toBe("Pets");
      expect(definitionList.text()).not.toContain("Pets");
      expect(definitionList.text()).toContain("human");
      const following = Node.DOCUMENT_POSITION_FOLLOWING;
      expect(
        definitionList.element.compareDocumentPosition(heading.element) &
          following
      ).toBeTruthy();
      expect(
        heading.element.compareDocumentPosition(list.element) & following
      ).toBeTruthy();
    }
  );

  it("renders a list at its field's position: fields before it, the list, then fields after it", async () => {
    const wrapper = mountRecord([nameColumn, petsColumn, speciesColumn], row);
    await flushPromises();

    const parts = Array.from(wrapper.get("section").element.children).map(
      (child) => child.tagName.toLowerCase()
    );
    expect(parts).toEqual(["dl", "div", "dl"]);
    const [before, listBlock, after] = Array.from(
      wrapper.get("section").element.children
    );
    expect(before?.textContent).toContain("Alice");
    expect(listBlock?.querySelector("h4")?.textContent?.trim()).toBe("Pets");
    expect(listBlock?.textContent).toContain("Bird 0");
    expect(after?.textContent).toContain("human");
  });

  it("filters the nested list to the rows that point back at this record", async () => {
    mountRecord([nameColumn, speciesColumn, petsColumn], row);
    await flushPromises();

    expect(fetchTableDataMock).toHaveBeenCalledWith(
      "test-schema",
      "Pet",
      expect.objectContaining({
        filter: { owner: { equals: [{ name: "Alice" }] } },
      })
    );
  });

  it("filters a REF_ARRAY list to the rows named by the ref values, key by key", async () => {
    const friendsColumn: IColumn = {
      id: "friends",
      label: "Friends",
      columnType: "REF_ARRAY",
      refTableId: "Pet",
      refSchemaId: "test-schema",
    };
    mount(DisplayRecord, {
      props: {
        columns: [nameColumn, friendsColumn],
        row: {
          name: "Alice",
          friends: [{ name: "Bird 0" }, { name: "Bird 1" }],
          friends_agg: { count: 2 },
        },
        showTableOfContents: false,
        lists: { friends: { layout: "LIST" } },
      },
    });
    await flushPromises();

    expect(fetchTableDataMock).toHaveBeenCalledWith(
      "test-schema",
      "Pet",
      expect.objectContaining({
        filter: {
          _or: [{ name: { equals: "Bird 0" } }, { name: { equals: "Bird 1" } }],
        },
      })
    );
  });

  it("passes the whole config entry to the list, not only its layout", async () => {
    const wrapper = mountRecord(
      [nameColumn, speciesColumn, petsColumn],
      row,
      "LIST",
      { layout: "LIST", titleColumn: "name" }
    );
    await flushPromises();

    expect(
      wrapper.findComponent(DisplayRecords).props("displayConfig")
    ).toEqual({
      layout: "LIST",
      titleColumn: "name",
    });
  });

  it("renders a section whose only field is a list without the list label", async () => {
    const wrapper = mountRecord([petsColumn], {
      pets: row.pets,
      pets_agg: row.pets_agg,
    });
    await flushPromises();

    const section = wrapper.get("section");
    expect(section.find("h4").exists()).toBe(false);
    expect(section.find("dl").exists()).toBe(false);
    expect(section.findComponent(RecordsList).text()).toContain("Bird 0");
  });

  it("drops a configured list whose count is zero: no list, no label", async () => {
    const wrapper = mountRecord([nameColumn, speciesColumn, petsColumn], {
      ...row,
      pets_agg: { count: 0 },
    });
    await flushPromises();

    expect(wrapper.findComponent(RecordsList).exists()).toBe(false);
    expect(wrapper.find("h4").exists()).toBe(false);
    expect(fetchTableDataMock).not.toHaveBeenCalled();
  });

  it("drops a zero-count list before grouping, so a section holding only it disappears", async () => {
    const wrapper = mountRecord([petsColumn], {
      pets: row.pets,
      pets_agg: { count: 0 },
    });
    await flushPromises();

    expect(wrapper.find("section").exists()).toBe(false);
  });

  it("renders a column without a config entry as today", async () => {
    const wrapper = mountRecord(
      [nameColumn, speciesColumn, petsColumn],
      row,
      null
    );
    await flushPromises();

    expect(wrapper.find("h4").exists()).toBe(false);
    expect(wrapper.get("dl").text()).toContain("Pets");
    expect(fetchTableDataMock).not.toHaveBeenCalled();
  });
});
