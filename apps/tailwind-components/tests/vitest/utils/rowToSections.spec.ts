import { describe, it, expect } from "vitest";
import { rowToSections } from "../../../utils/rowToSections";
import type {
  IRow,
  ITableMetaData,
} from "../../../../metadata-utils/src/types";

describe("rowToSections", () => {
  it("should return an empty array if metadata is not provided", () => {
    const result = rowToSections({ name: "Alice" }, null as any, {});
    expect(result).toEqual([]);
  });

  it("should group fields under headings", () => {
    const metadata: ITableMetaData = {
      columns: [
        { id: "heading1", label: "Personal Info", columnType: "HEADING" },
        { id: "name", label: "Name", columnType: "TEXT" },
        { id: "age", label: "Age", columnType: "INT" },
        { id: "heading2", label: "Contact Info", columnType: "HEADING" },
        { id: "email", label: "Email", columnType: "TEXT" },
      ],
      id: "",
      label: "",
      tableType: "",
    };

    const row: IRow = {
      name: "Alice",
      age: 30,
      email: "alice@example.com",
    };

    const result = rowToSections(row, metadata);

    expect(result).toEqual([
      {
        heading: "Personal Info",
        fields: [
          {
            key: "name",
            value: "Alice",
            metadata: metadata.columns[1],
          },
          {
            key: "age",
            value: 30,
            metadata: metadata.columns[2],
          },
        ],
      },
      {
        heading: "Contact Info",
        fields: [
          {
            key: "email",
            value: "alice@example.com",
            metadata: metadata.columns[4],
          },
        ],
      },
    ]);
  });

  it("should filter out mg_ fields unless showDataOwner is true", () => {
    const metadata: ITableMetaData = {
      columns: [
        { id: "mg_owner", label: "Owner", columnType: "TEXT" },
        { id: "name", label: "Name", columnType: "TEXT" },
      ],
      id: "",
      label: "",
      tableType: "",
    };

    const row: IRow = {
      name: "Alice",
      mg_owner: "Admin",
    };

    const resultWithoutOwner = rowToSections(row, metadata, {
      showDataOwner: false,
    });
    expect(resultWithoutOwner).toEqual([
      {
        heading: "",
        fields: [
          {
            key: "name",
            value: "Alice",
            metadata: metadata.columns[1],
          },
        ],
      },
    ]);

    const resultWithOwner = rowToSections(row, metadata, {
      showDataOwner: true,
    });
    expect(resultWithOwner).toEqual([
      {
        heading: "",
        fields: [
          {
            key: "mg_owner",
            value: "Admin",
            metadata: metadata.columns[0],
          },
          {
            key: "name",
            value: "Alice",
            metadata: metadata.columns[1],
          },
        ],
      },
    ]);
  });

  it("should skip fields not present in row unless they are HEADING", () => {
    const metadata: ITableMetaData = {
      columns: [
        { id: "heading1", label: "Group", columnType: "HEADING" },
        { id: "name", label: "Name", columnType: "TEXT" },
        { id: "missing", label: "Missing", columnType: "TEXT" },
      ],
      id: "",
      label: "",
      tableType: "",
    };

    const row: IRow = {
      name: "Alice",
    };

    const result = rowToSections(row, metadata);

    expect(result).toEqual([
      {
        heading: "Group",
        fields: [
          {
            key: "name",
            value: "Alice",
            metadata: metadata.columns[1],
          },
        ],
      },
    ]);
  });
});
