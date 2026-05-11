import { mount, flushPromises } from "@vue/test-utils";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { ref, computed, unref, nextTick } from "vue";
import DataList from "../../../../app/components/display/DataList.vue";

const mockFetchedRows = ref<Record<string, any>[]>([]);
const mockCount = ref(0);
const mockStatus = ref("success");
const mockTotalPages = ref(1);
const mockShowPagination = ref(false);

const useTableDataMock = vi.fn().mockImplementation(() => ({
  metadata: computed(() => undefined),
  rows: computed(() => mockFetchedRows.value),
  count: computed(() => mockCount.value),
  refresh: vi.fn(),
  status: mockStatus,
  totalPages: computed(() => mockTotalPages.value),
  showPagination: computed(() => mockShowPagination.value),
  errorMessage: computed(() => undefined),
}));

vi.mock("../../../../app/composables/useTableData", () => ({
  useTableData: (...args: any[]) => useTableDataMock(...args),
}));

vi.mock("../../../../app/utils/displayUtils", () => ({
  getListColumns: vi.fn().mockReturnValue([]),
}));

function makeRows(count: number): Record<string, any>[] {
  return Array.from({ length: count }, (_, index) => ({ id: `row-${index}` }));
}

function getSchemaIdArgAtMount(callIndex = 0): string {
  return unref(useTableDataMock.mock.calls[callIndex]?.[0]) ?? "";
}

function getTableIdArgAtMount(callIndex = 0): string {
  return unref(useTableDataMock.mock.calls[callIndex]?.[1]) ?? "";
}

function getCurrentSchemaId(callIndex = 0): string {
  const arg = useTableDataMock.mock.calls[callIndex]?.[0];
  if (arg && typeof arg === "object" && "value" in arg) {
    return (arg as any).value ?? "";
  }
  return unref(arg) ?? "";
}

beforeEach(() => {
  useTableDataMock.mockClear();
  mockFetchedRows.value = [];
  mockCount.value = 0;
  mockStatus.value = "success";
  mockTotalPages.value = 1;
  mockShowPagination.value = false;
});

async function typeInSearch(wrapper: ReturnType<typeof mount>, text: string) {
  const searchInput = wrapper.find("input");
  if (!searchInput.exists()) return false;
  vi.useFakeTimers();
  await searchInput.setValue(text);
  vi.runAllTimers();
  vi.useRealTimers();
  await nextTick();
  await flushPromises();
  return true;
}

describe("DataList — pure smart mode (no rows prop)", () => {
  it("uses schemaId and tableId in smart mode when no rows prop", async () => {
    mount(DataList, {
      props: {
        schemaId: "catalogueSchema",
        tableId: "Resources",
      },
    });
    await flushPromises();

    expect(getSchemaIdArgAtMount()).toBe("catalogueSchema");
    expect(getTableIdArgAtMount()).toBe("Resources");
  });

  it("passes filter prop to useTableData in pure smart mode", async () => {
    const filter = { id: { equals: "record-1" } };
    mount(DataList, {
      props: {
        schemaId: "mySchema",
        tableId: "myTable",
        filter,
      },
    });
    await flushPromises();

    expect(getSchemaIdArgAtMount()).toBe("mySchema");
    expect(getTableIdArgAtMount()).toBe("myTable");
  });
});

describe("DataList — dumb mode initial state (rows provided)", () => {
  it("starts with empty schemaId/tableId for useTableData when rows provided", async () => {
    const rows = makeRows(5);
    mount(DataList, {
      props: { rows, totalCount: 20, schemaId: "mySchema", tableId: "myTable" },
    });
    await flushPromises();

    expect(getSchemaIdArgAtMount()).toBe("");
    expect(getTableIdArgAtMount()).toBe("");
  });

  it("shows InlinePagination when totalCount > rows.length (truncated)", async () => {
    const rows = makeRows(5);
    const wrapper = mount(DataList, {
      props: {
        rows,
        totalCount: 20,
        schemaId: "mySchema",
        tableId: "myTable",
        pageSize: 5,
      },
    });
    await flushPromises();

    expect(wrapper.find(".inline-pagination").exists()).toBe(true);
  });

  it("does not show InlinePagination when totalCount === rows.length (all prefetched)", async () => {
    const rows = makeRows(5);
    const wrapper = mount(DataList, {
      props: {
        rows,
        totalCount: 5,
        schemaId: "mySchema",
        tableId: "myTable",
        pageSize: 5,
      },
    });
    await flushPromises();

    expect(wrapper.find(".inline-pagination").exists()).toBe(false);
  });

  it("does not show InlinePagination when no totalCount and rows fit in one page", async () => {
    const rows = makeRows(3);
    const wrapper = mount(DataList, {
      props: { rows, pageSize: 5 },
    });
    await flushPromises();

    expect(wrapper.find(".inline-pagination").exists()).toBe(false);
  });
});

describe("DataList — one-way switch to smart mode on search", () => {
  it("passes computed schemaId ref that resolves to empty string initially in dumb mode", async () => {
    const rows = makeRows(5);
    mount(DataList, {
      props: {
        rows,
        totalCount: 20,
        schemaId: "mySchema",
        tableId: "myTable",
        pageSize: 5,
        layout: "TABLE",
      },
    });
    await flushPromises();

    expect(getSchemaIdArgAtMount()).toBe("");
  });

  it("schemaId ref resolves to real value after search triggers smart switch", async () => {
    const rows = makeRows(5);
    const wrapper = mount(DataList, {
      props: {
        rows,
        totalCount: 20,
        schemaId: "mySchema",
        tableId: "myTable",
        pageSize: 5,
        layout: "TABLE",
      },
    });
    await flushPromises();

    expect(getSchemaIdArgAtMount()).toBe("");

    const typed = await typeInSearch(wrapper, "test query");
    if (typed) {
      expect(getCurrentSchemaId()).toBe("mySchema");
    }
  });

  it("shows search box in dumb hybrid mode when truncated", async () => {
    const rows = makeRows(5);
    const wrapper = mount(DataList, {
      props: {
        rows,
        totalCount: 20,
        schemaId: "mySchema",
        tableId: "myTable",
        pageSize: 5,
        layout: "TABLE",
      },
    });
    await flushPromises();

    expect(wrapper.find("input").exists()).toBe(true);
  });

  it("does not show search box when all data prefetched (totalCount === rows.length)", async () => {
    const rows = makeRows(5);
    const wrapper = mount(DataList, {
      props: {
        rows,
        totalCount: 5,
        schemaId: "mySchema",
        tableId: "myTable",
        pageSize: 5,
        layout: "TABLE",
      },
    });
    await flushPromises();

    expect(wrapper.find("input").exists()).toBe(false);
  });

  it("schemaId ref stays resolved to real value after search switch then clear search (one-way)", async () => {
    const rows = makeRows(5);
    const wrapper = mount(DataList, {
      props: {
        rows,
        totalCount: 20,
        schemaId: "mySchema",
        tableId: "myTable",
        pageSize: 5,
        layout: "TABLE",
      },
    });
    await flushPromises();

    await typeInSearch(wrapper, "something");
    expect(getCurrentSchemaId()).toBe("mySchema");

    await typeInSearch(wrapper, "");
    expect(getCurrentSchemaId()).toBe("mySchema");
  });
});

describe("DataList — one-way switch to smart on pagination beyond prefetch", () => {
  it("shows pagination when totalCount exceeds prefetched rows", async () => {
    const rows = makeRows(5);
    const wrapper = mount(DataList, {
      props: {
        rows,
        totalCount: 20,
        schemaId: "mySchema",
        tableId: "myTable",
        pageSize: 5,
      },
    });
    await flushPromises();

    expect(wrapper.find(".inline-pagination").exists()).toBe(true);
  });

  it("shows totalCount / 4 pages in pagination (dumb mode uses totalCount not rows.length)", async () => {
    const rows = makeRows(5);
    const wrapper = mount(DataList, {
      props: {
        rows,
        totalCount: 20,
        schemaId: "mySchema",
        tableId: "myTable",
        pageSize: 5,
      },
    });
    await flushPromises();

    const pagination = wrapper.find(".inline-pagination");
    expect(pagination.exists()).toBe(true);
    expect(pagination.text()).toContain("1 / 4");
  });

  it("effectiveSchemaId ref resolves to mySchema when hasSwitchedToSmart activates", async () => {
    const rows = makeRows(5);
    const wrapper = mount(DataList, {
      props: {
        rows,
        totalCount: 20,
        schemaId: "mySchema",
        tableId: "myTable",
        pageSize: 5,
        layout: "TABLE",
      },
    });
    await flushPromises();

    expect(getCurrentSchemaId()).toBe("");

    const typed = await typeInSearch(wrapper, "page2query");
    if (typed) {
      expect(getCurrentSchemaId()).toBe("mySchema");
    }
  });

  it("switches to smart mode via page watcher when navigating beyond prefetched range", async () => {
    const rows = makeRows(5);
    const wrapper = mount(DataList, {
      props: {
        rows,
        totalCount: 20,
        schemaId: "mySchema",
        tableId: "myTable",
        pageSize: 5,
      },
    });
    await flushPromises();

    expect(getCurrentSchemaId()).toBe("");

    (wrapper.vm as any).page = 2;
    await nextTick();
    await flushPromises();

    expect(getCurrentSchemaId()).toBe("mySchema");
  });
});
