import { describe, expect, test } from "vitest";
import type { ISchemaMetaData } from "../../../../metadata-utils/src/types";
import { enhanceMetadata } from "../../../app/utils/enhanceMetadata";

const schema: ISchemaMetaData = {
  id: "demo",
  label: "demo",
  tables: [
    {
      id: "Owner",
      label: "Owner",
      tableType: "DATA",
      schemaId: "demo",
      columns: [
        { id: "name", label: "Name", columnType: "STRING", key: 1 },
        { id: "pets", label: "Pets", columnType: "REFBACK", refTableId: "Pet" },
      ],
    },
    {
      id: "Pet",
      label: "Pet",
      tableType: "DATA",
      schemaId: "demo",
      columns: [
        { id: "name", label: "Name", columnType: "STRING", key: 1 },
        { id: "age", label: "Age", columnType: "INT" },
      ],
    },
  ],
};

describe("enhanceMetadata", () => {
  test("sets role and display from the config on the named columns, per table", () => {
    const enhanced = enhanceMetadata(schema, {
      Owner: { columns: { pets: { display: "CARDS", route: "pets" } } },
      Pet: {
        columns: { name: { role: "TITLE" }, age: { role: "DETAIL" } },
      },
    });

    const [owner, pet] = enhanced.tables;
    expect(owner?.columns.find((c) => c.id === "pets")?.display).toEqual(
      "CARDS"
    );
    expect(owner?.columns.find((c) => c.id === "name")?.role).toBeUndefined();
    expect(pet?.columns.map((c) => c.role)).toEqual(["TITLE", "DETAIL"]);
  });

  test("leaves the fetched metadata untouched, and carries no route into a column", () => {
    const enhanced = enhanceMetadata(schema, {
      Owner: { columns: { pets: { display: "LIST", route: "pets" } } },
    });

    expect(schema.tables[0]?.columns[1]).not.toHaveProperty("display");
    expect(enhanced.tables[0]?.columns[1]).not.toHaveProperty("route");
  });
});
