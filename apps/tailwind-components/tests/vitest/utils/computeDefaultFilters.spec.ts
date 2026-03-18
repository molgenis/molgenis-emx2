import { describe, it, expect } from "vitest";
import type { IColumn } from "../../../../metadata-utils/src/types";
import { computeDefaultFilters } from "../../../app/utils/computeDefaultFilters";

describe("computeDefaultFilters logic", () => {
  it("returns first 5 ontology columns", () => {
    const columns: IColumn[] = [
      { id: "ont1", label: "Ont 1", columnType: "ONTOLOGY" },
      { id: "ont2", label: "Ont 2", columnType: "ONTOLOGY_ARRAY" },
      { id: "ont3", label: "Ont 3", columnType: "ONTOLOGY" },
      { id: "ont4", label: "Ont 4", columnType: "ONTOLOGY" },
      { id: "ont5", label: "Ont 5", columnType: "ONTOLOGY" },
      { id: "ont6", label: "Ont 6", columnType: "ONTOLOGY" },
    ];

    const result = computeDefaultFilters(columns);
    expect(result).toEqual(["ont1", "ont2", "ont3", "ont4", "ont5"]);
  });

  it("fills with ref columns when < 5 ontology", () => {
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

  it("excludes mg_* columns", () => {
    const columns: IColumn[] = [
      { id: "mg_internal", label: "Internal", columnType: "ONTOLOGY" },
      { id: "ont1", label: "Ont 1", columnType: "ONTOLOGY" },
    ];

    const result = computeDefaultFilters(columns);
    expect(result).toEqual(["ont1"]);
  });

  it("returns empty array when no suitable columns", () => {
    const columns: IColumn[] = [
      { id: "str1", label: "String", columnType: "STRING" },
      { id: "int1", label: "Int", columnType: "INT" },
    ];

    const result = computeDefaultFilters(columns);
    expect(result).toEqual([]);
  });
});
