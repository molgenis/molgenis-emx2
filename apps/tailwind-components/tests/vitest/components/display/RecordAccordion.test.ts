import { mount } from "@vue/test-utils";
import { describe, expect, test } from "vitest";
import type {
  ColumnType,
  IColumn,
  IRow,
} from "../../../../../metadata-utils/src/types";
import RecordAccordion from "../../../../app/components/display/RecordAccordion.vue";

function column(id: string, columnType: ColumnType, label?: string): IColumn {
  return { id, label: label ?? id, columnType };
}

const columns: IColumn[] = [
  { ...column("name", "STRING", "Name"), key: 1 },
  column("about", "SECTION", "About"),
  column("diet", "STRING", "Diet"),
];

const row: IRow = { name: "spike", diet: "insects" };

function toggle(wrapper: ReturnType<typeof mount>) {
  const match = wrapper
    .findAll("button")
    .find((button) => button.attributes("aria-expanded") !== undefined);
  if (!match) {
    throw new Error("no accordion toggle button");
  }
  return match;
}

describe("RecordAccordion", () => {
  test("is closed by default", () => {
    const wrapper = mount(RecordAccordion, { props: { columns, row } });

    expect(toggle(wrapper).attributes("aria-expanded")).toBe("false");
  });

  test("heads the accordion with the record's primary key", () => {
    const wrapper = mount(RecordAccordion, { props: { columns, row } });

    expect(toggle(wrapper).text()).toBe("spike");
  });

  test("a passed titleTemplate wins over the derived key", () => {
    const wrapper = mount(RecordAccordion, {
      props: { columns, row, titleTemplate: "${name} the pet" },
    });

    expect(toggle(wrapper).text()).toBe("spike the pet");
  });

  test("a titleTemplate with no interpolation renders as its own literal text", () => {
    const wrapper = mount(RecordAccordion, {
      props: { columns, row, titleTemplate: "Custom label" },
    });

    expect(toggle(wrapper).text()).toBe("Custom label");
  });

  test("opening the accordion reveals the record's fields", async () => {
    const wrapper = mount(RecordAccordion, { props: { columns, row } });

    await toggle(wrapper).trigger("click");

    expect(toggle(wrapper).attributes("aria-expanded")).toBe("true");
    expect(wrapper.text()).toContain("insects");
  });

  test("showDetails off renders the label alone, with no accordion", () => {
    const wrapper = mount(RecordAccordion, {
      props: { columns, row, showDetails: false },
    });

    expect(wrapper.text()).toBe("spike");
    expect(wrapper.findAll("button")).toHaveLength(0);
  });
});
