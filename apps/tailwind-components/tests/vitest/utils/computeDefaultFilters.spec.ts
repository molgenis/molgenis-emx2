import { describe, it, expect } from "vitest";
import type { IColumn } from "../../../../metadata-utils/src/types";
import { computeDefaultFilters } from "../../../app/utils/computeDefaultFilters";

describe("computeDefaultFilters logic", () => {
  it("returns all ontology columns", () => {
    const columns: IColumn[] = [
      { id: "ont1", label: "Ont 1", columnType: "ONTOLOGY" },
      { id: "ont2", label: "Ont 2", columnType: "ONTOLOGY_ARRAY" },
      { id: "ont3", label: "Ont 3", columnType: "ONTOLOGY" },
      { id: "ont4", label: "Ont 4", columnType: "ONTOLOGY" },
      { id: "ont5", label: "Ont 5", columnType: "ONTOLOGY" },
      { id: "ont6", label: "Ont 6", columnType: "ONTOLOGY" },
    ];

    const result = computeDefaultFilters(columns);
    expect(result).toEqual(["ont1", "ont2", "ont3", "ont4", "ont5", "ont6"]);
  });

  it("returns all columns including ontology and ref types", () => {
    const columns: IColumn[] = [
      { id: "ont1", label: "Ont 1", columnType: "ONTOLOGY" },
      { id: "ont2", label: "Ont 2", columnType: "ONTOLOGY_ARRAY" },
      { id: "ref1", label: "Ref 1", columnType: "REF" },
      { id: "ref2", label: "Ref 2", columnType: "REF_ARRAY" },
      { id: "ref3", label: "Ref 3", columnType: "REF" },
    ];

    const result = computeDefaultFilters(columns);
    expect(result).toEqual(["ont1", "ont2", "ref1", "ref2", "ref3"]);
  });

  it("includes SELECT columns as default filters", () => {
    const columns: IColumn[] = [
      { id: "ont1", label: "Ont 1", columnType: "ONTOLOGY" },
      { id: "sel1", label: "Select 1", columnType: "SELECT" },
      { id: "sel2", label: "Select 2", columnType: "SELECT" },
    ];

    const result = computeDefaultFilters(columns);
    expect(result).toEqual(["ont1", "sel1", "sel2"]);
  });

  it("excludes HEADING and SECTION columns", () => {
    const columns: IColumn[] = [
      { id: "heading1", label: "Heading", columnType: "HEADING" },
      { id: "section1", label: "Section", columnType: "SECTION" },
      { id: "ont1", label: "Ont 1", columnType: "ONTOLOGY" },
    ];

    const result = computeDefaultFilters(columns);
    expect(result).toEqual(["ont1"]);
  });

  it("excludes FILE columns", () => {
    const columns: IColumn[] = [
      { id: "file1", label: "File", columnType: "FILE" },
      { id: "ont1", label: "Ont 1", columnType: "ONTOLOGY" },
    ];

    const result = computeDefaultFilters(columns);
    expect(result).toEqual(["ont1"]);
  });

  it("excludes mg_* columns", () => {
    const columns: IColumn[] = [
      { id: "mg_internal", label: "Internal", columnType: "ONTOLOGY" },
      { id: "ont1", label: "Ont 1", columnType: "ONTOLOGY" },
    ];

    const result = computeDefaultFilters(columns);
    expect(result).toEqual(["ont1"]);
  });

  it("excludes STRING columns from defaults", () => {
    const columns: IColumn[] = [
      { id: "str1", label: "String", columnType: "STRING" },
      { id: "int1", label: "Int", columnType: "INT" },
    ];

    const result = computeDefaultFilters(columns);
    expect(result).toEqual(["int1"]);
  });

  it("excludes all string-like types (TEXT, EMAIL, HYPERLINK, UUID, AUTO_ID)", () => {
    const columns: IColumn[] = [
      { id: "text1", label: "Text", columnType: "TEXT" },
      { id: "email1", label: "Email", columnType: "EMAIL" },
      { id: "link1", label: "Link", columnType: "HYPERLINK" },
      { id: "uuid1", label: "UUID", columnType: "UUID" },
      { id: "auto1", label: "Auto ID", columnType: "AUTO_ID" },
      { id: "bool1", label: "Bool", columnType: "BOOL" },
      { id: "int1", label: "Int", columnType: "INT" },
    ];

    const result = computeDefaultFilters(columns);
    expect(result).toEqual(["bool1", "int1"]);
  });
});
