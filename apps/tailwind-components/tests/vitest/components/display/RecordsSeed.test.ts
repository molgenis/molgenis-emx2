import { enableAutoUnmount, flushPromises, mount } from "@vue/test-utils";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { clearNuxtData } from "#app";
import type { IColumn, IRow } from "../../../../../metadata-utils/src/types";
import Records from "../../../../app/components/display/Records.vue";
import Pagination from "../../../../app/components/Pagination.vue";

enableAutoUnmount(afterEach);

const fetchTableDataMock = vi.fn();
const fetchTableMetadataMock = vi.fn();

vi.mock("../../../../app/composables/fetchTableData", () => ({
  default: (...args: unknown[]) => fetchTableDataMock(...args),
}));
vi.mock("../../../../app/composables/fetchTableMetadata", () => ({
  default: (...args: unknown[]) => fetchTableMetadataMock(...args),
}));

const columns: IColumn[] = [
  { id: "name", label: "Name", columnType: "STRING", key: 1 },
];

function rows(first: number, count: number): IRow[] {
  return Array.from({ length: count }, (_, index) => ({
    name: `Bird ${first + index}`,
  }));
}

describe("Records.vue seeded from a parent query", () => {
  beforeEach(() => {
    clearNuxtData();
    fetchTableDataMock.mockReset();
    fetchTableMetadataMock.mockReset();
    fetchTableMetadataMock.mockResolvedValue({ id: "pet", columns });
    fetchTableDataMock.mockResolvedValue({ rows: rows(3, 3), count: 6 });
  });

  function mountSeeded() {
    return mount(Records, {
      props: {
        schemaId: "test-schema",
        tableId: "pet",
        pageSize: 3,
        seed: { columns, rows: rows(0, 3), count: 6 },
      },
    });
  }

  it("renders the seed rows and the seed count and requests nothing", async () => {
    const wrapper = mountSeeded();
    await flushPromises();

    expect(wrapper.text()).toContain("Bird 0");
    expect(wrapper.text()).toContain("1 - 3 of 6");
    expect(fetchTableMetadataMock).not.toHaveBeenCalled();
    expect(fetchTableDataMock).not.toHaveBeenCalled();
  });

  it("fetches only the data of page 2, once, when the reader pages", async () => {
    const wrapper = mountSeeded();
    await flushPromises();

    wrapper.findComponent(Pagination).vm.$emit("update", 2);
    await flushPromises();

    expect(fetchTableMetadataMock).not.toHaveBeenCalled();
    expect(fetchTableDataMock).toHaveBeenCalledTimes(1);
    expect(fetchTableDataMock).toHaveBeenCalledWith(
      "test-schema",
      "pet",
      expect.objectContaining({ limit: 3, offset: 3 })
    );
    expect(wrapper.text()).toContain("Bird 3");
    expect(wrapper.text()).toContain("4 - 6 of 6");
  });

  it("without a seed still fetches metadata and page 1", async () => {
    const wrapper = mount(Records, {
      props: { schemaId: "test-schema", tableId: "pet", pageSize: 3 },
    });
    await flushPromises();

    expect(fetchTableMetadataMock).toHaveBeenCalledTimes(1);
    expect(fetchTableDataMock).toHaveBeenCalledTimes(1);
    expect(wrapper.text()).toContain("Bird 3");
  });
});
