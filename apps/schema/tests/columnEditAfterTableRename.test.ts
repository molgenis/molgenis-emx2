import { mount, VueWrapper } from "@vue/test-utils";
import { describe, expect, test } from "vitest";
import SchemaView from "../src/components/SchemaView.vue";

function petStore(withSubclass: boolean) {
  const table: Record<string, any> = {
    name: "Pet",
    oldName: "Pet",
    tableType: "DATA",
    columns: [
      {
        name: "name",
        oldName: "name",
        table: "Pet",
        columnType: "STRING",
        position: 1,
        key: 1,
      },
      {
        name: "weight",
        oldName: "weight",
        table: "Pet",
        columnType: "DECIMAL",
        position: 2,
      },
    ],
  };
  if (withSubclass) {
    table.subclasses = [
      { name: "Cat", oldName: "Cat", inheritName: "Pet", columns: [] },
    ];
    table.columns.push({
      name: "whiskers",
      oldName: "whiskers",
      table: "Cat",
      columnType: "STRING",
      position: 3,
    });
  }
  return { name: "pet store", tables: [table], ontologies: [] };
}

function mountSchemaEditor(withSubclass: boolean) {
  return mount(SchemaView, {
    props: {
      modelValue: petStore(withSubclass),
      schemaNames: ["pet store"],
      isManager: true,
      locales: ["en"],
    },
  });
}

function editPencils(wrapper: VueWrapper) {
  return wrapper
    .findAll("button")
    .filter((button) => button.find("i.fa-pencil-alt").exists());
}

async function renameTable(
  wrapper: VueWrapper,
  subclassRow: boolean,
  newName: string
) {
  await editPencils(wrapper)[subclassRow ? 1 : 0].trigger("click");
  await wrapper.find("input#table_name").setValue(newName);
  await wrapper
    .findAll("button")
    .filter((button) => button.text() === "Apply")[0]
    .trigger("click");
}

async function openColumnEditModal(
  wrapper: VueWrapper,
  withSubclass: boolean,
  columnIndex: number
) {
  const columnPencils = editPencils(wrapper).slice(withSubclass ? 2 : 1);
  await columnPencils[columnIndex].trigger("click");
}

function editedColumnName(wrapper: VueWrapper) {
  return (wrapper.find("input#column_name").element as HTMLInputElement).value;
}

function subclassShownInModal(wrapper: VueWrapper) {
  return (wrapper.find("input#column_table").element as HTMLInputElement).value;
}

function subclassColumnOf(wrapper: VueWrapper, rowIndex: number) {
  return wrapper
    .findAll("table.table-bordered")
    .at(-1)!
    .findAll("tbody tr")
    [rowIndex].findAll("td")[1]
    .text();
}

describe("editing a column after its table was renamed", () => {
  test("a column of a renamed table can still be edited", async () => {
    const wrapper = mountSchemaEditor(false);

    await renameTable(wrapper, false, "PetRenamed");
    await openColumnEditModal(wrapper, false, 1);

    expect(editedColumnName(wrapper)).toBe("weight");
  });

  test("a column of a table renamed twice can still be edited", async () => {
    const wrapper = mountSchemaEditor(false);

    await renameTable(wrapper, false, "PetRenamed");
    await renameTable(wrapper, false, "PetRenamedAgain");
    await openColumnEditModal(wrapper, false, 0);

    expect(editedColumnName(wrapper)).toBe("name");
  });

  test("a column owned by a subclass can be edited", async () => {
    const wrapper = mountSchemaEditor(true);

    await openColumnEditModal(wrapper, true, 2);

    expect(editedColumnName(wrapper)).toBe("whiskers");
    expect(subclassShownInModal(wrapper)).toBe("Cat");
  });

  test("a column of a renamed subclass can still be edited", async () => {
    const wrapper = mountSchemaEditor(true);

    await renameTable(wrapper, true, "Kitten");
    await openColumnEditModal(wrapper, true, 2);

    expect(editedColumnName(wrapper)).toBe("whiskers");
    expect(subclassShownInModal(wrapper)).toBe("Kitten");
  });

  test("renaming a table keeps the subclass shown for each of its columns", async () => {
    const wrapper = mountSchemaEditor(true);

    await renameTable(wrapper, false, "PetRenamed");

    expect([0, 1, 2].map((row) => subclassColumnOf(wrapper, row))).toEqual([
      "PetRenamed",
      "PetRenamed",
      "Cat",
    ]);
  });
});
