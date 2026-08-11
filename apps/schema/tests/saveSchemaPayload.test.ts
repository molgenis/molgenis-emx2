import { flushPromises, mount, VueWrapper } from "@vue/test-utils";
import { beforeEach, describe, expect, test, vi } from "vitest";
import VueScrollTo from "vue-scrollto";
import Schema from "../src/components/Schema.vue";

const request = vi.fn();

vi.mock("graphql-request", () => ({
  request: (...args: unknown[]) => request(...args),
}));

function petStoreResponse() {
  return {
    _session: { schemas: ["pet store", "zoo"], roles: ["Manager"] },
    _schema: {
      name: "pet store",
      tables: [
        {
          name: "Pet",
          tableType: "DATA",
          columns: [
            {
              name: "name",
              table: "Pet",
              columnType: "STRING",
              position: 1,
              key: 1,
            },
          ],
        },
        {
          name: "Kitten",
          tableType: "DATA",
          inheritNames: ["Pet"],
          columns: [],
        },
        {
          name: "HouseCat",
          tableType: "DATA",
          inheritNames: ["Lion", "Tiger"],
          inheritSchemaName: "zoo",
          columns: [
            {
              name: "whiskers",
              table: "HouseCat",
              columnType: "STRING",
              position: 1,
            },
          ],
        },
      ],
    },
  };
}

async function mountSchemaEditor() {
  const wrapper = mount(Schema, {
    global: { plugins: [VueScrollTo] },
  });
  await flushPromises();
  return wrapper;
}

async function editFirstTableName(wrapper: VueWrapper, newName: string) {
  await wrapper
    .findAll("button")
    .filter((button) => button.find("i.fa-pencil-alt").exists())[0]
    .trigger("click");
  await wrapper.find("input#table_name").setValue(newName);
  await wrapper
    .findAll("button")
    .filter((button) => button.text() === "Apply")[0]
    .trigger("click");
}

async function saveSchema(wrapper: VueWrapper) {
  await wrapper
    .findAll("button")
    .filter((button) => button.text() === "Save")[0]
    .trigger("click");
  await flushPromises();
}

function savedTables(): Record<string, any>[] {
  const mutationCall = request.mock.calls.find(
    (call) => call[2] !== undefined
  ) as unknown[];
  return (mutationCall[2] as { tables: Record<string, any>[] }).tables;
}

describe("saving the schema editor", () => {
  beforeEach(() => {
    request.mockReset();
    request.mockImplementation(async (_url, _query, variables) =>
      variables === undefined ? petStoreResponse() : {}
    );
  });

  test("a multi-parent table is saved with inheritNames and inheritSchemaName only, never the singular inheritName", async () => {
    const wrapper = await mountSchemaEditor();

    await editFirstTableName(wrapper, "Pets");
    await saveSchema(wrapper);

    const houseCat = savedTables().find((table) => table.name === "HouseCat")!;
    expect(houseCat.inheritNames).toEqual(["Lion", "Tiger"]);
    expect("inheritName" in houseCat).toBe(false);
    expect(houseCat.inheritSchemaName).toBe("zoo");
    expect(
      savedTables()
        .filter((table) => "inheritName" in table)
        .map((table) => table.name)
    ).toEqual([]);
  });
});
