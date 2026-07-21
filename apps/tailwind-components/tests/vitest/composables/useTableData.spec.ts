// @vitest-environment happy-dom
import { describe, it, expect, vi, beforeEach } from "vitest";
import { ref, nextTick } from "vue";
import { flushPromises } from "@vue/test-utils";
import type { ITableMetaData } from "../../../../metadata-utils/src/types";

vi.mock("../../../app/composables/fetchTableMetadata", () => ({
  default: vi.fn(),
}));

vi.mock("../../../app/composables/fetchTableData", () => ({
  default: vi.fn(),
}));

import fetchTableMetadata from "../../../app/composables/fetchTableMetadata";
import fetchTableData from "../../../app/composables/fetchTableData";
import { useTableData } from "../../../app/composables/useTableData";

const mockMetadata: ITableMetaData = {
  id: "Pet",
  schemaId: "pet store",
  name: "Pet",
  label: "Pet",
  tableType: "DATA",
  columns: [{ id: "name", columnType: "STRING", label: "Name", key: 1 }],
};

const mockTableDataResult = {
  rows: [{ name: "Fluffy" }, { name: "Buddy" }],
  count: 2,
};

beforeEach(() => {
  vi.clearAllMocks();
  (fetchTableMetadata as ReturnType<typeof vi.fn>).mockResolvedValue(
    mockMetadata
  );
  (fetchTableData as ReturnType<typeof vi.fn>).mockResolvedValue(
    mockTableDataResult
  );
});

describe("useTableData — empty schemaId/tableId guard", () => {
  it("stays idle and does not fetch when schemaId is empty string", async () => {
    const { status } = useTableData("", "Pet", {
      pageSize: 10,
      page: ref(1),
    });
    await flushPromises();
    expect(status.value).toBe("idle");
    expect(fetchTableData).not.toHaveBeenCalled();
    expect(fetchTableMetadata).not.toHaveBeenCalled();
  });

  it("stays idle and does not fetch when tableId is empty string", async () => {
    const { status } = useTableData("pet store", "", {
      pageSize: 10,
      page: ref(1),
    });
    await flushPromises();
    expect(status.value).toBe("idle");
    expect(fetchTableData).not.toHaveBeenCalled();
  });

  it("stays idle when both schemaId and tableId are empty", async () => {
    const { status } = useTableData("", "", { pageSize: 10, page: ref(1) });
    await flushPromises();
    expect(status.value).toBe("idle");
  });
});

describe("useTableData — successful fetch", () => {
  it("sets status to success and populates rows and count", async () => {
    const { status, rows, count } = useTableData("pet store", "Pet", {
      pageSize: 10,
      page: ref(1),
    });
    await flushPromises();
    expect(status.value).toBe("success");
    expect(rows.value).toEqual([{ name: "Fluffy" }, { name: "Buddy" }]);
    expect(count.value).toBe(2);
  });

  it("passes schemaId, tableId, limit and offset to fetchTableData", async () => {
    useTableData("pet store", "Pet", { pageSize: 5, page: ref(2) });
    await flushPromises();
    expect(fetchTableData).toHaveBeenCalledWith(
      "pet store",
      "Pet",
      expect.objectContaining({ limit: 5, offset: 5 })
    );
  });

  it("passes searchTerms to fetchTableData", async () => {
    const searchTerms = ref("fluffy");
    useTableData("pet store", "Pet", {
      pageSize: 10,
      page: ref(1),
      searchTerms,
    });
    await flushPromises();
    expect(fetchTableData).toHaveBeenCalledWith(
      "pet store",
      "Pet",
      expect.objectContaining({ searchTerms: "fluffy" })
    );
  });

  it("populates metadata from fetchTableMetadata result", async () => {
    const { metadata } = useTableData("pet store", "Pet", {
      pageSize: 10,
      page: ref(1),
    });
    await flushPromises();
    expect(metadata.value).toEqual(mockMetadata);
  });
});

describe("useTableData — computed values", () => {
  it("totalPages = Math.ceil(count / pageSize)", async () => {
    (fetchTableData as ReturnType<typeof vi.fn>).mockResolvedValue({
      rows: [],
      count: 25,
    });
    const { totalPages } = useTableData("pet store", "Pet", {
      pageSize: 10,
      page: ref(1),
    });
    await flushPromises();
    expect(totalPages.value).toBe(3);
  });

  it("showPagination is true when count > pageSize", async () => {
    (fetchTableData as ReturnType<typeof vi.fn>).mockResolvedValue({
      rows: [],
      count: 15,
    });
    const { showPagination } = useTableData("pet store", "Pet", {
      pageSize: 10,
      page: ref(1),
    });
    await flushPromises();
    expect(showPagination.value).toBe(true);
  });

  it("showPagination is false when count <= pageSize", async () => {
    (fetchTableData as ReturnType<typeof vi.fn>).mockResolvedValue({
      rows: [],
      count: 5,
    });
    const { showPagination } = useTableData("pet store", "Pet", {
      pageSize: 10,
      page: ref(1),
    });
    await flushPromises();
    expect(showPagination.value).toBe(false);
  });

  it("rows defaults to empty array when fetchTableData returns non-array", async () => {
    (fetchTableData as ReturnType<typeof vi.fn>).mockResolvedValue({
      rows: null,
      count: 0,
    });
    const { rows } = useTableData("pet store", "Pet", {
      pageSize: 10,
      page: ref(1),
    });
    await flushPromises();
    expect(rows.value).toEqual([]);
  });

  it("count defaults to 0 when fetchTableData returns undefined count", async () => {
    (fetchTableData as ReturnType<typeof vi.fn>).mockResolvedValue({
      rows: [],
      count: undefined,
    });
    const { count } = useTableData("pet store", "Pet", {
      pageSize: 10,
      page: ref(1),
    });
    await flushPromises();
    expect(count.value).toBe(0);
  });
});

describe("useTableData — error handling", () => {
  it("sets status to error and populates errorMessage when fetch throws", async () => {
    (fetchTableData as ReturnType<typeof vi.fn>).mockRejectedValue(
      new Error("Network error")
    );
    const { status, errorMessage } = useTableData("pet store", "Pet", {
      pageSize: 10,
      page: ref(1),
    });
    await flushPromises();
    expect(status.value).toBe("error");
    expect(errorMessage.value).toBe("Network error");
  });

  it("sets errorMessage to Unknown error for non-Error rejections", async () => {
    (fetchTableData as ReturnType<typeof vi.fn>).mockRejectedValue("boom");
    const { errorMessage } = useTableData("pet store", "Pet", {
      pageSize: 10,
      page: ref(1),
    });
    await flushPromises();
    expect(errorMessage.value).toBe("Unknown error");
  });
});

describe("useTableData — watch triggers", () => {
  it("re-fetches when page changes", async () => {
    const page = ref(1);
    useTableData("pet store", "Pet", { pageSize: 10, page });
    await flushPromises();
    const callsBefore = (fetchTableData as ReturnType<typeof vi.fn>).mock.calls
      .length;

    page.value = 2;
    await nextTick();
    await flushPromises();
    expect(
      (fetchTableData as ReturnType<typeof vi.fn>).mock.calls.length
    ).toBeGreaterThan(callsBefore);
  });

  it("re-fetches when searchTerms changes", async () => {
    const searchTerms = ref("");
    useTableData("pet store", "Pet", {
      pageSize: 10,
      page: ref(1),
      searchTerms,
    });
    await flushPromises();
    const callsBefore = (fetchTableData as ReturnType<typeof vi.fn>).mock.calls
      .length;

    searchTerms.value = "fluffy";
    await nextTick();
    await flushPromises();
    expect(
      (fetchTableData as ReturnType<typeof vi.fn>).mock.calls.length
    ).toBeGreaterThan(callsBefore);
  });
});

describe("useTableData — Ref<string> inputs", () => {
  it("accepts Ref<string> for schemaId and tableId", async () => {
    const schemaId = ref("pet store");
    const tableId = ref("Pet");
    const { status } = useTableData(schemaId, tableId, {
      pageSize: 10,
      page: ref(1),
    });
    await flushPromises();
    expect(status.value).toBe("success");
  });

  it("stays idle when schemaId Ref is empty, fetches after it is set", async () => {
    const schemaId = ref("");
    const tableId = ref("Pet");
    const { status } = useTableData(schemaId, tableId, {
      pageSize: 10,
      page: ref(1),
    });
    await flushPromises();
    expect(status.value).toBe("idle");

    schemaId.value = "pet store";
    await nextTick();
    await flushPromises();
    expect(status.value).toBe("success");
  });

  it("stays idle when tableId Ref is empty, fetches after it is set", async () => {
    const schemaId = ref("pet store");
    const tableId = ref("");
    const { status } = useTableData(schemaId, tableId, {
      pageSize: 10,
      page: ref(1),
    });
    await flushPromises();
    expect(status.value).toBe("idle");

    tableId.value = "Pet";
    await nextTick();
    await flushPromises();
    expect(status.value).toBe("success");
  });
});

describe("useTableData — refresh function", () => {
  it("calling refresh re-fetches data", async () => {
    const { refresh } = useTableData("pet store", "Pet", {
      pageSize: 10,
      page: ref(1),
    });
    await flushPromises();
    const callsBefore = (fetchTableData as ReturnType<typeof vi.fn>).mock.calls
      .length;

    await refresh();
    expect(
      (fetchTableData as ReturnType<typeof vi.fn>).mock.calls.length
    ).toBeGreaterThan(callsBefore);
  });
});
