import { mockNuxtImport } from "@nuxt/test-utils/runtime";
import { describe, expect, it } from "vitest";
import { getPrimaryKey } from "../../../app/utils/getPrimaryKey";

const schema = {
  id: "pet store",
  tables: [
    {
      id: "Sample",
      columns: [
        { id: "id", columnType: "INT", key: 1 },
        { id: "label", columnType: "STRING" },
      ],
    },
    {
      id: "Owner",
      columns: [
        { id: "firstName", columnType: "STRING", key: 1 },
        { id: "number", columnType: "INT", key: 1 },
      ],
    },
    {
      id: "Pet",
      columns: [
        { id: "name", columnType: "STRING", key: 1 },
        { id: "owner", columnType: "REF", key: 1, refTableId: "Owner" },
        { id: "weight", columnType: "DECIMAL" },
      ],
    },
  ],
};

mockNuxtImport("fetchMetadata", () => async () => schema);

describe("getPrimaryKey", () => {
  it("should keep an integer key value", async () => {
    expect(
      await getPrimaryKey({ id: 42, label: "x" }, "Sample", "pet store")
    ).toEqual({ id: 42 });
  });

  it("should keep an integer key value of 0", async () => {
    expect(await getPrimaryKey({ id: 0 }, "Sample", "pet store")).toEqual({
      id: 0,
    });
  });

  it("should keep a numeric part of a composite ref key", async () => {
    expect(
      await getPrimaryKey(
        { name: "pooky", owner: { firstName: "Jane", number: 7 }, weight: 1 },
        "Pet",
        "pet store"
      )
    ).toEqual({ name: "pooky", owner: { firstName: "Jane", number: 7 } });
  });

  it("should give integer keyed rows distinct identifiers", async () => {
    const first = await getPrimaryKey({ id: 1 }, "Sample", "pet store");
    const second = await getPrimaryKey({ id: 2 }, "Sample", "pet store");
    expect(JSON.stringify(first)).not.toEqual(JSON.stringify(second));
  });
});
