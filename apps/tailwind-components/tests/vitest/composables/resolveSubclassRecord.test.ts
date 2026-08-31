import { describe, expect, it, vi } from "vitest";
import resolveSubclassRecord from "../../../app/composables/resolveSubclassRecord";
import type { ITableMetaData } from "../../../../metadata-utils/src/types";

const collectionsMetadata = { id: "Collections" } as ITableMetaData;

describe("resolveSubclassRecord", () => {
  it("resolves nothing when mg_tableclass names the route table (same class)", async () => {
    const fetchMetadata = vi.fn();
    const fetchRow = vi.fn();

    const result = await resolveSubclassRecord(
      "catalogue-demo",
      "Resources",
      { mg_tableclass: "catalogue-demo.Resources" },
      { id: "RAINE" },
      fetchMetadata,
      fetchRow
    );

    expect(result).toBeNull();
    expect(fetchMetadata).not.toHaveBeenCalled();
    expect(fetchRow).not.toHaveBeenCalled();
  });

  it("fetches the subclass table's metadata and row when mg_tableclass names a different table", async () => {
    const subclassRow = {
      id: "RAINE",
      mg_tableclass: "catalogue-demo.Collections",
    };
    const fetchMetadata = vi.fn().mockResolvedValue(collectionsMetadata);
    const fetchRow = vi.fn().mockResolvedValue(subclassRow);

    const result = await resolveSubclassRecord(
      "catalogue-demo",
      "Resources",
      { mg_tableclass: "catalogue-demo.Collections" },
      { id: "RAINE" },
      fetchMetadata,
      fetchRow
    );

    expect(result).toEqual({
      tableMetadata: collectionsMetadata,
      row: subclassRow,
    });
    expect(fetchMetadata).toHaveBeenCalledWith("catalogue-demo", "Collections");
    expect(fetchRow).toHaveBeenCalledWith("catalogue-demo", "Collections", {
      id: "RAINE",
    });
  });

  it("resolves nothing when the row carries no mg_tableclass field (missing field)", async () => {
    const fetchMetadata = vi.fn();
    const fetchRow = vi.fn();

    const result = await resolveSubclassRecord(
      "catalogue-demo",
      "Resources",
      { id: "RAINE" },
      { id: "RAINE" },
      fetchMetadata,
      fetchRow
    );

    expect(result).toBeNull();
    expect(fetchMetadata).not.toHaveBeenCalled();
    expect(fetchRow).not.toHaveBeenCalled();
  });

  it("resolves nothing when the subclass table's metadata fails to load (unknown table)", async () => {
    const fetchMetadata = vi.fn().mockRejectedValue(new Error("not found"));
    const fetchRow = vi.fn();

    const result = await resolveSubclassRecord(
      "catalogue-demo",
      "Resources",
      { mg_tableclass: "catalogue-demo.NoSuchTable" },
      { id: "RAINE" },
      fetchMetadata,
      fetchRow
    );

    expect(result).toBeNull();
    expect(fetchRow).not.toHaveBeenCalled();
  });
});
