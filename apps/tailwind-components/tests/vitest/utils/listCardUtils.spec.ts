import { describe, it, expect } from "vitest";
import {
  getDetailColumns,
  getDescriptionColumn,
  getLogoColumn,
} from "../../../app/utils/displayUtils";
import type { IColumn } from "../../../../metadata-utils/src/types";

function col(overrides: Partial<IColumn>): IColumn {
  return {
    id: "test",
    label: "Test",
    columnType: "STRING",
    ...overrides,
  } as IColumn;
}

describe("getDetailColumns", () => {
  it("returns columns with role DETAIL when any exist", () => {
    const columns = [
      col({ id: "name", columnType: "STRING" }),
      col({ id: "summary", columnType: "TEXT", role: "DETAIL" }),
      col({ id: "tags", columnType: "STRING", role: "DETAIL" }),
    ];
    const data = { name: "Alice", summary: "Bio text", tags: "tag1" };
    const result = getDetailColumns(columns, data);
    expect(result.map((c) => c.id)).toEqual(["summary", "tags"]);
  });

  it("excludes DETAIL columns with null data", () => {
    const columns = [
      col({ id: "summary", columnType: "TEXT", role: "DETAIL" }),
      col({ id: "tags", columnType: "STRING", role: "DETAIL" }),
    ];
    const data = { summary: null, tags: "tag1" };
    const result = getDetailColumns(columns, data);
    expect(result.map((c) => c.id)).toEqual(["tags"]);
  });

  it("falls back to non-key, non-role, non-heading columns when no DETAIL roles", () => {
    const columns = [
      col({ id: "name", columnType: "STRING", key: 1 }),
      col({ id: "description", columnType: "TEXT" }),
      col({ id: "status", columnType: "STRING" }),
    ];
    const data = { name: "Alice", description: "A desc", status: "active" };
    const result = getDetailColumns(columns, data);
    expect(result.map((c) => c.id)).toEqual(["description", "status"]);
  });

  it("excludes TITLE, DESCRIPTION, LOGO roles from fallback", () => {
    const columns = [
      col({ id: "title", columnType: "STRING", role: "TITLE" }),
      col({ id: "desc", columnType: "TEXT", role: "DESCRIPTION" }),
      col({ id: "logo", columnType: "FILE", role: "LOGO" }),
      col({ id: "status", columnType: "STRING" }),
    ];
    const data = {
      title: "My Title",
      desc: "Some desc",
      logo: { url: "/img.png" },
      status: "active",
    };
    const result = getDetailColumns(columns, data);
    expect(result.map((c) => c.id)).toEqual(["status"]);
  });

  it("caps fallback at 5 columns", () => {
    const columns = Array.from({ length: 8 }, (_, i) =>
      col({ id: `field${i}`, columnType: "STRING" })
    );
    const data = Object.fromEntries(columns.map((c) => [c.id, `val${c.id}`]));
    const result = getDetailColumns(columns, data);
    expect(result).toHaveLength(5);
  });

  it("excludes columns with null or undefined data from fallback", () => {
    const columns = [
      col({ id: "name", columnType: "STRING" }),
      col({ id: "description", columnType: "TEXT" }),
      col({ id: "status", columnType: "STRING" }),
    ];
    const data = { name: "Alice", description: null, status: undefined };
    const result = getDetailColumns(columns, data);
    expect(result.map((c) => c.id)).toEqual(["name"]);
  });

  it("excludes HEADING and SECTION column types from fallback", () => {
    const columns = [
      col({ id: "section1", columnType: "HEADING" }),
      col({ id: "section2", columnType: "SECTION" }),
      col({ id: "name", columnType: "STRING" }),
    ];
    const data = { section1: "x", section2: "y", name: "Alice" };
    const result = getDetailColumns(columns, data);
    expect(result.map((c) => c.id)).toEqual(["name"]);
  });

  it("excludes mg_ prefixed columns from fallback", () => {
    const columns = [
      col({ id: "mg_insertedOn", columnType: "DATETIME" }),
      col({ id: "name", columnType: "STRING" }),
    ];
    const data = { mg_insertedOn: "2024-01-01", name: "Alice" };
    const result = getDetailColumns(columns, data);
    expect(result.map((c) => c.id)).toEqual(["name"]);
  });

  it("returns empty array when no columns provided", () => {
    const result = getDetailColumns([], {});
    expect(result).toEqual([]);
  });

  it("prefers explicit DETAIL over fallback even when only one DETAIL column", () => {
    const columns = [
      col({ id: "summary", columnType: "TEXT", role: "DETAIL" }),
      col({ id: "status", columnType: "STRING" }),
      col({ id: "country", columnType: "STRING" }),
    ];
    const data = { summary: "Bio", status: "active", country: "NL" };
    const result = getDetailColumns(columns, data);
    expect(result.map((c) => c.id)).toEqual(["summary"]);
  });
});

describe("getDescriptionColumn", () => {
  it("returns column with DESCRIPTION role that has data", () => {
    const columns = [
      col({ id: "name", columnType: "STRING" }),
      col({ id: "bio", columnType: "TEXT", role: "DESCRIPTION" }),
    ];
    const data = { name: "Alice", bio: "A biography" };
    const result = getDescriptionColumn(columns, data);
    expect(result?.id).toBe("bio");
  });

  it("returns undefined when DESCRIPTION column has null data", () => {
    const columns = [
      col({ id: "bio", columnType: "TEXT", role: "DESCRIPTION" }),
    ];
    const data = { bio: null };
    const result = getDescriptionColumn(columns, data);
    expect(result).toBeUndefined();
  });

  it("returns undefined when no DESCRIPTION role column exists", () => {
    const columns = [col({ id: "name", columnType: "STRING" })];
    const data = { name: "Alice" };
    const result = getDescriptionColumn(columns, data);
    expect(result).toBeUndefined();
  });
});

describe("getLogoColumn", () => {
  it("returns column with LOGO role that has data", () => {
    const columns = [
      col({ id: "name", columnType: "STRING" }),
      col({ id: "logo", columnType: "FILE", role: "LOGO" }),
    ];
    const data = { name: "Alice", logo: { url: "/img.png" } };
    const result = getLogoColumn(columns, data);
    expect(result?.id).toBe("logo");
  });

  it("returns undefined when LOGO column has null data", () => {
    const columns = [col({ id: "logo", columnType: "FILE", role: "LOGO" })];
    const data = { logo: null };
    const result = getLogoColumn(columns, data);
    expect(result).toBeUndefined();
  });

  it("returns undefined when no LOGO role column exists", () => {
    const columns = [col({ id: "name", columnType: "STRING" })];
    const data = { name: "Alice" };
    const result = getLogoColumn(columns, data);
    expect(result).toBeUndefined();
  });
});
