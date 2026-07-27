import { describe, it, expect } from "vitest";
import {
  filterTablesByTypeAndRole,
  type SchemaTableEntry,
} from "../../../app/utils/groupTablesByRole";

const dataMain: SchemaTableEntry = {
  id: "resources",
  label: "Resources",
  tableType: "DATA",
  role: "MAIN",
  description: "",
};

const dataDetail: SchemaTableEntry = {
  id: "collectionEvents",
  label: "Collection events",
  tableType: "DATA",
  role: "DETAIL",
  description: "",
};

const dataNoRole: SchemaTableEntry = {
  id: "publications",
  label: "Publications",
  tableType: "DATA",
  role: undefined,
  description: "",
};

const dataRoleNull: SchemaTableEntry = {
  id: "contacts",
  label: "Contacts",
  tableType: "DATA",
  role: null,
  description: "",
};

const ontologyMain: SchemaTableEntry = {
  id: "diseases",
  label: "Diseases",
  tableType: "ONTOLOGIES",
  role: "MAIN",
  description: "",
};

const ontologyDetail: SchemaTableEntry = {
  id: "subpopulations",
  label: "Subpopulations",
  tableType: "ONTOLOGIES",
  role: "DETAIL",
  description: "",
};

describe("filterTablesByTypeAndRole", () => {
  it("returns empty array for empty input", () => {
    expect(filterTablesByTypeAndRole([], "DATA", "MAIN")).toEqual([]);
    expect(filterTablesByTypeAndRole([], "DATA", "DETAIL")).toEqual([]);
  });

  it("treats role undefined as MAIN", () => {
    const result = filterTablesByTypeAndRole([dataNoRole], "DATA", "MAIN");
    expect(result).toHaveLength(1);
    expect(result[0].id).toBe("publications");
  });

  it("treats role null as MAIN", () => {
    const result = filterTablesByTypeAndRole([dataRoleNull], "DATA", "MAIN");
    expect(result).toHaveLength(1);
    expect(result[0].id).toBe("contacts");
  });

  it("includes tables with role MAIN in MAIN group", () => {
    const result = filterTablesByTypeAndRole([dataMain], "DATA", "MAIN");
    expect(result).toHaveLength(1);
    expect(result[0].id).toBe("resources");
  });

  it("includes tables with role DETAIL (uppercase, as returned by GraphQL) in DETAIL group", () => {
    const result = filterTablesByTypeAndRole([dataDetail], "DATA", "DETAIL");
    expect(result).toHaveLength(1);
    expect(result[0].id).toBe("collectionEvents");
  });

  it("excludes DETAIL tables from MAIN group", () => {
    const result = filterTablesByTypeAndRole(
      [dataMain, dataDetail, dataNoRole],
      "DATA",
      "MAIN"
    );
    expect(result.map((t) => t.id)).not.toContain("collectionEvents");
    expect(result).toHaveLength(2);
  });

  it("excludes MAIN and unset-role tables from DETAIL group", () => {
    const result = filterTablesByTypeAndRole(
      [dataMain, dataDetail, dataNoRole],
      "DATA",
      "DETAIL"
    );
    expect(result).toHaveLength(1);
    expect(result[0].id).toBe("collectionEvents");
  });

  it("filters by tableType: DATA tables excluded from ONTOLOGIES group", () => {
    const result = filterTablesByTypeAndRole(
      [dataMain, ontologyMain],
      "ONTOLOGIES",
      "MAIN"
    );
    expect(result).toHaveLength(1);
    expect(result[0].id).toBe("diseases");
  });

  it("filters by tableType: ONTOLOGIES DETAIL tables in correct group", () => {
    const result = filterTablesByTypeAndRole(
      [dataDetail, ontologyDetail],
      "ONTOLOGIES",
      "DETAIL"
    );
    expect(result).toHaveLength(1);
    expect(result[0].id).toBe("subpopulations");
  });

  it("sorts results alphabetically by label within each group", () => {
    const tables: SchemaTableEntry[] = [
      {
        id: "z",
        label: "Zebra",
        tableType: "DATA",
        role: undefined,
        description: "",
      },
      {
        id: "a",
        label: "Apple",
        tableType: "DATA",
        role: undefined,
        description: "",
      },
      {
        id: "m",
        label: "Mango",
        tableType: "DATA",
        role: "MAIN",
        description: "",
      },
    ];
    const result = filterTablesByTypeAndRole(tables, "DATA", "MAIN");
    expect(result.map((t) => t.label)).toEqual(["Apple", "Mango", "Zebra"]);
  });
});
