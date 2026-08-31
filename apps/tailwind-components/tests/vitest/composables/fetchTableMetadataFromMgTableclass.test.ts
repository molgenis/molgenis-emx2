import { beforeEach, afterEach, describe, expect, test, vi } from "vitest";
import type { ITableMetaData } from "../../../../metadata-utils/src/types";
import fetchTableMetadataFromMgTableclass from "../../../app/composables/fetchTableMetadataFromMgTableclass";

const tableA: ITableMetaData = {
  id: "TableA",
  schemaId: "schemaA",
  name: "TableA",
  label: "TableA",
  tableType: "DATA",
  columns: [],
};

describe("fetchTableMetadataFromMgTableclass", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    vi.stubGlobal("$fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  // fetchMetadata caches by schemaId, so each test that hits $fetch uses a schemaId no other test uses.
  test("resolves the named table", async () => {
    fetchMock.mockResolvedValueOnce({
      data: { _schema: { id: "schemaA", label: "schemaA", tables: [tableA] } },
    });

    await expect(
      fetchTableMetadataFromMgTableclass("schemaA.TableA")
    ).resolves.toEqual(tableA);
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  test("returns undefined for undefined input", async () => {
    await expect(
      fetchTableMetadataFromMgTableclass(undefined)
    ).resolves.toBeUndefined();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  test("returns undefined when there is no dot", async () => {
    await expect(
      fetchTableMetadataFromMgTableclass("schemaOnly")
    ).resolves.toBeUndefined();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  test("returns undefined when the table id is empty", async () => {
    await expect(
      fetchTableMetadataFromMgTableclass("schemaB.")
    ).resolves.toBeUndefined();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  test("returns undefined for a table the schema does not have", async () => {
    fetchMock.mockResolvedValueOnce({
      data: { _schema: { id: "schemaC", label: "schemaC", tables: [] } },
    });

    await expect(
      fetchTableMetadataFromMgTableclass("schemaC.NoSuchTable")
    ).resolves.toBeUndefined();
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });
});
