import { describe, it, expect } from "vitest";
import { getSubclassColumns } from "../../app/composables/getSubclassColumns";
import type { ITableMetaData } from "../../../metadata-utils/src/types";

const person: ITableMetaData = {
  id: "Person",
  name: "Person",
  schemaId: "test",
  label: "Person",
  tableType: "DATA",
  columns: [{ id: "name", label: "Name", columnType: "STRING" }],
};

const employee: ITableMetaData = {
  id: "Employee",
  name: "Employee",
  schemaId: "test",
  label: "Employee",
  tableType: "DATA",
  inheritId: "Person",
  columns: [
    { id: "name", label: "Name", columnType: "STRING" },
    { id: "salary", label: "Salary", columnType: "INT" },
  ],
};

const manager: ITableMetaData = {
  id: "Manager",
  name: "Manager",
  schemaId: "test",
  label: "Manager",
  tableType: "DATA",
  inheritId: "Employee",
  columns: [
    { id: "name", label: "Name", columnType: "STRING" },
    { id: "salary", label: "Salary", columnType: "INT" },
    { id: "department", label: "Department", columnType: "STRING" },
  ],
};

const student: ITableMetaData = {
  id: "Student",
  name: "Student",
  schemaId: "test",
  label: "Student",
  tableType: "DATA",
  inheritId: "Person",
  columns: [
    { id: "name", label: "Name", columnType: "STRING" },
    { id: "studentId", label: "Student ID", columnType: "STRING" },
  ],
};

describe("getSubclassColumns", () => {
  it("returns empty array when table has no subclasses", () => {
    const result = getSubclassColumns("Person", [person]);
    expect(result).toEqual([]);
  });

  it("returns empty array when tableId is not found", () => {
    const result = getSubclassColumns("Unknown", [person, employee]);
    expect(result).toEqual([]);
  });

  it("returns subclass-only columns tagged with sourceTableId", () => {
    const result = getSubclassColumns("Person", [person, employee]);
    expect(result).toHaveLength(1);
    expect(result[0].id).toBe("salary");
    expect(result[0].sourceTableId).toBe("Employee");
    expect(result[0].columnType).toBe("INT");
  });

  it("does not return columns already present in parent", () => {
    const result = getSubclassColumns("Person", [person, employee]);
    const ids = result.map((c) => c.id);
    expect(ids).not.toContain("name");
  });

  it("collects columns from multi-level subclass hierarchy", () => {
    const result = getSubclassColumns("Person", [person, employee, manager]);
    const ids = result.map((c) => c.id);
    expect(ids).toContain("salary");
    expect(ids).toContain("department");
    expect(ids).not.toContain("name");
  });

  it("tags deep subclass columns with their own sourceTableId", () => {
    const result = getSubclassColumns("Person", [person, employee, manager]);
    const dept = result.find((c) => c.id === "department");
    expect(dept?.sourceTableId).toBe("Manager");
  });

  it("collects columns from multiple sibling subclasses", () => {
    const result = getSubclassColumns("Person", [person, employee, student]);
    const ids = result.map((c) => c.id);
    expect(ids).toContain("salary");
    expect(ids).toContain("studentId");
  });

  it("deduplicates columns when same id appears in sibling subclasses", () => {
    const employeeWithExtra: ITableMetaData = {
      ...employee,
      columns: [
        { id: "name", label: "Name", columnType: "STRING" },
        { id: "salary", label: "Salary", columnType: "INT" },
        { id: "sharedField", label: "Shared", columnType: "STRING" },
      ],
    };
    const studentWithExtra: ITableMetaData = {
      ...student,
      columns: [
        { id: "name", label: "Name", columnType: "STRING" },
        { id: "studentId", label: "Student ID", columnType: "STRING" },
        { id: "sharedField", label: "Shared", columnType: "STRING" },
      ],
    };

    const result = getSubclassColumns("Person", [
      person,
      employeeWithExtra,
      studentWithExtra,
    ]);
    const sharedOccurrences = result.filter((c) => c.id === "sharedField");
    expect(sharedOccurrences).toHaveLength(1);
  });
});
