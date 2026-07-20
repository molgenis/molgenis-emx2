import axios from "axios";
import { beforeEach, describe, expect, test, vi } from "vitest";
import { fetchTablePermissions, resolveTablePermission } from "./client";

vi.mock("axios");

const permissions = [
  { id: "Pet", name: "Pet", canInsert: true, canUpdate: false },
];

describe("fetchTablePermissions", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    (axios.post as any).mockResolvedValue({
      data: { data: { _session: { tablePermissions: permissions } } },
    });
  });

  test("returns the table permissions of the requested schema", async () => {
    const result = await fetchTablePermissions("schema-returns");
    expect(result).toEqual(permissions);
    expect(axios.post).toHaveBeenCalledWith(
      "/schema-returns/graphql",
      expect.objectContaining({ query: expect.stringContaining("_session") })
    );
  });

  test("caches per schema so repeated calls hit the network once", async () => {
    await fetchTablePermissions("schema-cache");
    await fetchTablePermissions("schema-cache");
    expect(axios.post).toHaveBeenCalledTimes(1);
  });

  test("fetches separately for different schemas", async () => {
    await fetchTablePermissions("schema-a");
    await fetchTablePermissions("schema-b");
    expect(axios.post).toHaveBeenCalledTimes(2);
  });

  test("returns an empty list when the session has no permissions", async () => {
    (axios.post as any).mockResolvedValue({
      data: { data: { _session: {} } },
    });
    const result = await fetchTablePermissions("schema-empty");
    expect(result).toEqual([]);
  });
});

describe("resolveTablePermission", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    (axios.post as any).mockResolvedValue({
      data: { data: { _session: { tablePermissions: permissions } } },
    });
  });

  test("resolves from the seed (current schema) without fetching", async () => {
    const result = await resolveTablePermission("pet store", "Pet", [
      { id: "Pet", name: "Pet", canInsert: true } as any,
    ]);
    expect(result?.canInsert).toBe(true);
    expect(axios.post).not.toHaveBeenCalled();
  });

  test("fetches the referenced schema when the table is not in the seed", async () => {
    const result = await resolveTablePermission("resolve-other", "Pet", [
      { id: "Order", name: "Order", canInsert: true } as any,
    ]);
    expect(axios.post).toHaveBeenCalledWith(
      "/resolve-other/graphql",
      expect.anything()
    );
    expect(result?.canInsert).toBe(true);
  });

  test("matches on table name when the id is pascal-cased", async () => {
    const result = await resolveTablePermission("pet store", "my table", [
      { id: "MyTable", name: "my table", canInsert: true } as any,
    ]);
    expect(result?.canInsert).toBe(true);
    expect(axios.post).not.toHaveBeenCalled();
  });

  test("returns undefined when the referenced table has no permission", async () => {
    (axios.post as any).mockResolvedValue({
      data: { data: { _session: { tablePermissions: [] } } },
    });
    const result = await resolveTablePermission("resolve-missing", "Pet", []);
    expect(result).toBeUndefined();
  });

  test("does not fetch when no schema is given and the seed misses", async () => {
    const result = await resolveTablePermission("", "Pet", []);
    expect(result).toBeUndefined();
    expect(axios.post).not.toHaveBeenCalled();
  });
});
