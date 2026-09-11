import { describe, expect, test } from "vitest";
import type { IColumn } from "../../../metadata-utils/src/types";
import { recordTitle } from "../../../app/utils/recordTitle";

describe("recordTitle", () => {
  test("joins every key column's value", () => {
    const columns: IColumn[] = [
      { id: "name", label: "Name", columnType: "STRING", key: 1 },
      { id: "category", label: "Category", columnType: "STRING", key: 1 },
      { id: "diet", label: "Diet", columnType: "STRING" },
    ];

    expect(
      recordTitle(columns, {
        name: "spike",
        category: "dog",
        diet: "insects",
      })
    ).toBe("spike - dog");
  });

  test("flattens a ref key value into text", () => {
    const columns: IColumn[] = [
      { id: "category", label: "Category", columnType: "REF", key: 1 },
    ];

    expect(recordTitle(columns, { category: { name: "dog" } })).toBe("dog");
  });

  test("is empty when the row carries no key value", () => {
    const columns: IColumn[] = [
      { id: "name", label: "Name", columnType: "STRING", key: 1 },
    ];

    expect(recordTitle(columns, {})).toBe("");
    expect(recordTitle(columns, null)).toBe("");
  });

  test("a key value of 0 or false still renders", () => {
    const columns: IColumn[] = [
      { id: "rank", label: "Rank", columnType: "INT", key: 1 },
      { id: "active", label: "Active", columnType: "BOOL", key: 1 },
    ];

    expect(recordTitle(columns, { rank: 0, active: false })).toBe("0 - false");
  });

  test("renders an interpolated template when one is given", () => {
    const columns: IColumn[] = [
      { id: "name", label: "Name", columnType: "STRING", key: 1 },
    ];

    expect(recordTitle(columns, { name: "spike" }, "${name} the pet")).toBe(
      "spike the pet"
    );
  });

  test("renders empty, not undefined, when the template fails to interpolate", () => {
    // A row carrying an "id" key of null drives columnValueToString to its
    // undefined-returning branch once the template throws.
    const columns: IColumn[] = [
      { id: "id", label: "Id", columnType: "STRING", key: 1 },
    ];

    expect(recordTitle(columns, { id: null }, "${missing}")).toBe("");
  });

  test("a plain-text template renders literally; one with a stray backtick throws and falls through to the flattened row", () => {
    const columns: IColumn[] = [
      { id: "breed", label: "Breed", columnType: "STRING" },
    ];

    expect(recordTitle(columns, { breed: "terrier" }, "Favorite Pet")).toBe(
      "Favorite Pet"
    );
    expect(recordTitle(columns, { breed: "terrier" }, "It`s a pet")).toBe(
      " terrier"
    );
  });
});
