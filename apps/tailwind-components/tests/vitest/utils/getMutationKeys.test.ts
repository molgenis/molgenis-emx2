import { describe, expect, it } from "vitest";
import { getMutationKeys } from "../../../app/utils/getMutationKeys";

describe("getMutationKeys", () => {
  const response = {
    data: {
      insert: {
        message: "inserted 2 records to Pet",
        keys: [
          { table: "Owner", keys: ['{"name":"anna"}'] },
          { table: "Pet", keys: ['{"id":"a1"}', '{"id":"b2"}'] },
        ],
      },
    },
  };

  it("returns the parsed keys of the table, in input order", () => {
    expect(getMutationKeys(response, "insert", "Pet")).toEqual([
      { id: "a1" },
      { id: "b2" },
    ]);
  });

  it("keeps nested reference keys", () => {
    const nested = {
      data: {
        insert: {
          keys: [{ table: "Ownership", keys: ['{"owner":{"name":"anna"}}'] }],
        },
      },
    };
    expect(getMutationKeys(nested, "insert", "Ownership")).toEqual([
      { owner: { name: "anna" } },
    ]);
  });

  it("returns an empty list for another table or mutation", () => {
    expect(getMutationKeys(response, "insert", "Order")).toEqual([]);
    expect(getMutationKeys(response, "update", "Pet")).toEqual([]);
  });

  it("returns an empty list when the server sends no keys", () => {
    expect(
      getMutationKeys({ data: { insert: { message: "ok" } } }, "insert", "Pet")
    ).toEqual([]);
    expect(getMutationKeys(undefined, "insert", "Pet")).toEqual([]);
  });
});
