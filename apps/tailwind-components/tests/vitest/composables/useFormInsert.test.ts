import { mockNuxtImport } from "@nuxt/test-utils/runtime";
import { beforeEach, describe, expect, test, vi } from "vitest";
import { ref } from "vue";
import type { ITableMetaData } from "../../../../metadata-utils/src";
import type { columnValue } from "../../../../metadata-utils/src/types";
import useForm from "../../../app/composables/useForm";

const petMetadata: ITableMetaData = {
  id: "Pet",
  name: "Pet",
  schemaId: "pet store",
  label: "Pet",
  tableType: "DATA",
  columns: [
    { id: "id", label: "id", columnType: "AUTO_ID", key: 1 },
    { id: "name", label: "name", columnType: "STRING" },
  ],
};

mockNuxtImport("fetchMetadata", () => async () => ({
  id: "pet store",
  tables: [petMetadata],
}));

describe("useForm insertInto", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("$fetch", fetchMock);
  });

  test("asks for the keys and puts the generated AUTO_ID in the form", async () => {
    fetchMock.mockResolvedValue({
      data: {
        insert: {
          message: "inserted 1 records to Pet",
          keys: [{ table: "Pet", keys: ['{"id":"generated-1"}'] }],
        },
      },
    });
    const formValues = ref<Record<string, columnValue>>({ name: "tom" });
    const form = useForm(ref(petMetadata), formValues);

    await form.insertInto();

    const body = fetchMock.mock.calls[0]?.[1]?.body as FormData;
    expect(body.get("query")).toContain("keys{table keys}");
    expect(formValues.value.id).toBe("generated-1");
    expect(form.rowKey.value).toEqual({ id: "generated-1" });
  });

  test("keeps the form values when the server returns no keys", async () => {
    fetchMock.mockResolvedValue({ data: { insert: { message: "ok" } } });
    const formValues = ref<Record<string, columnValue>>({ name: "tom" });
    const form = useForm(ref(petMetadata), formValues);

    await form.insertInto();

    expect(formValues.value.name).toBe("tom");
    expect(formValues.value.id ?? null).toBeNull();
  });
});
