import { describe, expect, test, vi, beforeEach, afterEach } from "vitest";
import { ref, nextTick } from "vue";
import { flushPromises } from "@vue/test-utils";
import type { ITableMetaData } from "../../../../metadata-utils/src";

const mockMetadata: ITableMetaData = {
  id: "TestTable",
  name: "TestTable",
  schemaId: "TestSchema",
  label: "Test Table",
  tableType: "DATA",
  columns: [
    { id: "id", label: "ID", columnType: "STRING", key: 1 },
    { id: "name", label: "Name", columnType: "STRING" },
  ],
};

const mockRows = [
  { id: "1", name: "Row 1" },
  { id: "2", name: "Row 2" },
];

const mockFetchTableMetadata = vi.fn();
const mockFetchTableData = vi.fn();

vi.mock("#imports", () => ({
  fetchTableMetadata: (...args: any[]) => mockFetchTableMetadata(...args),
  fetchTableData: (...args: any[]) => mockFetchTableData(...args),
}));

vi.mock("../../../app/composables/fetchTableMetadata", () => ({
  default: (...args: any[]) => mockFetchTableMetadata(...args),
}));

vi.mock("../../../app/composables/fetchTableData", () => ({
  default: (...args: any[]) => mockFetchTableData(...args),
}));

import { useTableData } from "../../../app/composables/useTableData";

describe("useTableData", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockFetchTableMetadata.mockResolvedValue(mockMetadata);
    mockFetchTableData.mockResolvedValue({ rows: mockRows, count: 2 });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  test("should fetch data and return success status", async () => {
    const page = ref(1);
    const { status, metadata, rows, count } = useTableData(
      "TestSchema",
      "TestTable",
      { pageSize: 10, page }
    );

    // initial status should be pending since fetch starts immediately
    expect(status.value).toBe("pending");

    // wait for async operations to complete
    await flushPromises();

    expect(status.value).toBe("success");
    expect(metadata.value).toEqual(mockMetadata);
    expect(rows.value).toEqual(mockRows);
    expect(count.value).toBe(2);
  });

  test("should call fetchTableMetadata and fetchTableData with correct params", async () => {
    const page = ref(1);
    const searchTerms = ref("test search");
    const orderby = ref<{ column: string; direction: "ASC" | "DESC" }>({
      column: "name",
      direction: "ASC",
    });

    useTableData("TestSchema", "TestTable", {
      pageSize: 20,
      page,
      searchTerms,
      orderby,
    });

    await flushPromises();

    expect(mockFetchTableMetadata).toHaveBeenCalledWith(
      "TestSchema",
      "TestTable"
    );
    expect(mockFetchTableData).toHaveBeenCalledWith(
      "TestSchema",
      "TestTable",
      expect.objectContaining({
        limit: 20,
        offset: 0,
        orderby: { name: "ASC" },
        searchTerms: "test search",
      })
    );
  });

  test("should trigger refresh on page change", async () => {
    const page = ref(1);
    useTableData("TestSchema", "TestTable", {
      pageSize: 10,
      page,
    });

    await flushPromises();
    expect(mockFetchTableData).toHaveBeenCalledTimes(1);

    // change page
    page.value = 2;
    await nextTick();
    await flushPromises();

    expect(mockFetchTableData).toHaveBeenCalledTimes(2);
    expect(mockFetchTableData).toHaveBeenLastCalledWith(
      "TestSchema",
      "TestTable",
      expect.objectContaining({ offset: 10 })
    );
  });

  test("should trigger refresh on searchTerms change", async () => {
    const page = ref(1);
    const searchTerms = ref("");
    useTableData("TestSchema", "TestTable", {
      pageSize: 10,
      page,
      searchTerms,
    });

    await flushPromises();
    expect(mockFetchTableData).toHaveBeenCalledTimes(1);

    // change search
    searchTerms.value = "new search";
    await nextTick();
    await flushPromises();

    expect(mockFetchTableData).toHaveBeenCalledTimes(2);
    expect(mockFetchTableData).toHaveBeenLastCalledWith(
      "TestSchema",
      "TestTable",
      expect.objectContaining({ searchTerms: "new search" })
    );
  });

  test("should trigger refresh on orderby change", async () => {
    const page = ref(1);
    const orderby = ref<{ column: string; direction: "ASC" | "DESC" }>({
      column: "id",
      direction: "ASC",
    });
    useTableData("TestSchema", "TestTable", {
      pageSize: 10,
      page,
      orderby,
    });

    await flushPromises();
    expect(mockFetchTableData).toHaveBeenCalledTimes(1);

    // change orderby
    orderby.value = { column: "name", direction: "DESC" };
    await nextTick();
    await flushPromises();

    expect(mockFetchTableData).toHaveBeenCalledTimes(2);
    expect(mockFetchTableData).toHaveBeenLastCalledWith(
      "TestSchema",
      "TestTable",
      expect.objectContaining({ orderby: { name: "DESC" } })
    );
  });

  test("refresh function should re-fetch data", async () => {
    const page = ref(1);
    const { refresh } = useTableData("TestSchema", "TestTable", {
      pageSize: 10,
      page,
    });

    await flushPromises();
    expect(mockFetchTableData).toHaveBeenCalledTimes(1);

    // manual refresh
    await refresh();

    expect(mockFetchTableData).toHaveBeenCalledTimes(2);
  });

  test("should set status to error on fetch failure", async () => {
    mockFetchTableData.mockRejectedValueOnce(new Error("Network error"));

    const page = ref(1);
    const { status } = useTableData("TestSchema", "TestTable", {
      pageSize: 10,
      page,
    });

    await flushPromises();
    expect(status.value).toBe("error");
  });

  test("should handle empty rows response", async () => {
    mockFetchTableData.mockResolvedValueOnce({ rows: [], count: 0 });

    const page = ref(1);
    const { status, rows, count } = useTableData("TestSchema", "TestTable", {
      pageSize: 10,
      page,
    });

    await flushPromises();
    expect(status.value).toBe("success");
    expect(rows.value).toEqual([]);
    expect(count.value).toBe(0);
  });

  test("should handle undefined rows in response", async () => {
    mockFetchTableData.mockResolvedValueOnce({ rows: undefined, count: 0 });

    const page = ref(1);
    const { status, rows } = useTableData("TestSchema", "TestTable", {
      pageSize: 10,
      page,
    });

    await flushPromises();
    expect(status.value).toBe("success");
    expect(rows.value).toEqual([]);
  });

  test("status should be pending during fetch", async () => {
    let resolvePromise: (value: any) => void;
    const pendingPromise = new Promise((resolve) => {
      resolvePromise = resolve;
    });

    mockFetchTableData.mockReturnValueOnce(pendingPromise);

    const page = ref(1);
    const { status } = useTableData("TestSchema", "TestTable", {
      pageSize: 10,
      page,
    });

    // should be pending while waiting
    expect(status.value).toBe("pending");

    // resolve the promise
    resolvePromise!({ rows: mockRows, count: 2 });
    await flushPromises();

    expect(status.value).toBe("success");
  });

  test("should compute totalPages correctly", async () => {
    mockFetchTableData.mockResolvedValueOnce({ rows: mockRows, count: 25 });
    const page = ref(1);
    const { totalPages } = useTableData("TestSchema", "TestTable", {
      pageSize: 10,
      page,
    });
    await flushPromises();
    expect(totalPages.value).toBe(3); // ceil(25/10) = 3
  });

  test("should compute showPagination correctly", async () => {
    mockFetchTableData.mockResolvedValueOnce({ rows: mockRows, count: 5 });
    const page = ref(1);
    const { showPagination } = useTableData("TestSchema", "TestTable", {
      pageSize: 10,
      page,
    });
    await flushPromises();
    expect(showPagination.value).toBe(false); // 5 <= 10
  });

  test("should set errorMessage on fetch failure", async () => {
    mockFetchTableData.mockRejectedValueOnce(new Error("Network error"));
    const page = ref(1);
    const { status, errorMessage } = useTableData("TestSchema", "TestTable", {
      pageSize: 10,
      page,
    });
    await flushPromises();
    expect(status.value).toBe("error");
    expect(errorMessage.value).toBe("Network error");
  });

  test("should clear errorMessage on successful refetch", async () => {
    mockFetchTableData.mockRejectedValueOnce(new Error("Network error"));
    const page = ref(1);
    const { status, errorMessage, refresh } = useTableData(
      "TestSchema",
      "TestTable",
      {
        pageSize: 10,
        page,
      }
    );
    await flushPromises();
    expect(errorMessage.value).toBe("Network error");

    // reset mock to succeed
    mockFetchTableData.mockResolvedValueOnce({ rows: mockRows, count: 2 });
    await refresh();

    expect(status.value).toBe("success");
    expect(errorMessage.value).toBeUndefined();
  });
});
