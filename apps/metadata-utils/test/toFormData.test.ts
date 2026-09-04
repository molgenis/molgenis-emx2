import { afterAll, beforeAll, describe, expect, it, vi } from "vitest";
import { toFormData } from "../src/toFormData";

beforeAll(() => {
  vi.stubGlobal("window", { File });
});

afterAll(() => {
  vi.unstubAllGlobals();
});

describe("toFormData", () => {
  it("strips mg computed fields and includes uploaded files in the FormData payload", () => {
    const file = new File(["hello world"], "hello.txt", { type: "text/plain" });
    const rowData: Record<string, unknown> = {
      id: "abc123",
      label: "Example",
      file,
      mg_insertedBy: "system",
      mg_insertedOn: "2024-01-01T00:00:00Z",
      mg_updatedBy: "system",
      mg_updatedOn: "2024-01-02T00:00:00Z",
    };

    const formData = toFormData(rowData as Record<string, any>);
    const variables = JSON.parse(formData.get("variables") as string);
    const payload = variables.value[0];

    expect(payload).toMatchObject({
      id: "abc123",
      label: "Example",
    });
    expect(payload).not.toHaveProperty("mg_insertedBy");
    expect(payload).not.toHaveProperty("mg_insertedOn");
    expect(payload).not.toHaveProperty("mg_updatedBy");
    expect(payload).not.toHaveProperty("mg_updatedOn");
    expect(payload.file).toEqual(expect.any(String));
    expect(formData.get(payload.file)).toBe(file);
  });

  it("keeps mg computed fields when stripMgComputedFields is false", () => {
    const formData = toFormData(
      {
        id: "abc123",
        mg_insertedBy: "system",
        mg_insertedOn: "2024-01-01T00:00:00Z",
      },
      false
    );

    const variables = JSON.parse(formData.get("variables") as string);

    expect(variables.value[0]).toEqual({
      id: "abc123",
      mg_insertedBy: "system",
      mg_insertedOn: "2024-01-01T00:00:00Z",
    });
  });
});
