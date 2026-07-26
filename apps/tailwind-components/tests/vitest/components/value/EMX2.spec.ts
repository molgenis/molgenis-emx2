import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import ValueEMX2 from "../../../../app/components/value/EMX2.vue";
import type { IColumn } from "../../../../../metadata-utils/src/types";

const singleOntologyColumn: IColumn = {
  id: "tissue",
  label: "Tissue",
  columnType: "ONTOLOGY",
  refTableId: "Tissues",
  refSchemaId: "catalogue",
};

const ontologyArrayColumn: IColumn = {
  id: "tissues",
  label: "Tissues",
  columnType: "ONTOLOGY_ARRAY",
  refTableId: "Tissues",
  refSchemaId: "catalogue",
};

const stringArrayColumn: IColumn = {
  id: "aliases",
  label: "Aliases",
  columnType: "STRING_ARRAY",
};

const refbackColumn: IColumn = {
  id: "tables",
  label: "Tables",
  columnType: "REFBACK",
  refTableId: "Tables",
  refSchemaId: "catalogue",
  refLabelDefault: "${name}",
} as IColumn;

const sevenNames = ["a", "b", "c", "d", "e", "f", "g"];

describe("value/EMX2.vue multi valued dispatch", () => {
  it("renders a multi valued column stored on the row inline, capped at five values", () => {
    const wrapper = mount(ValueEMX2, {
      props: { metadata: stringArrayColumn, data: sevenNames },
    });

    expect(wrapper.text()).toBe("a, b, c, d, e Show 2 more");
  });

  it("renders a refback as its own uncapped collection, not as an inline list", () => {
    const wrapper = mount(ValueEMX2, {
      props: {
        metadata: refbackColumn,
        data: sevenNames.map((name) => ({ name })),
      },
    });

    expect(wrapper.text()).toBe("a  ,b  ,c  ,d  ,e  ,f  ,g");
  });
});

describe("value/EMX2.vue ontology dispatch", () => {
  it("renders a single ONTOLOGY value as one clickable term", () => {
    const wrapper = mount(ValueEMX2, {
      props: { metadata: singleOntologyColumn, data: { name: "Blood" } },
    });

    expect(wrapper.text()).toBe("Blood");
  });

  it("emits valueClick with the column and term when a single ONTOLOGY value is clicked", async () => {
    const wrapper = mount(ValueEMX2, {
      props: { metadata: singleOntologyColumn, data: { name: "Blood" } },
    });

    await wrapper.find("span.text-link").trigger("click");

    expect(wrapper.emitted("valueClick")).toEqual([
      [{ metadata: singleOntologyColumn, data: { name: "Blood" } }],
    ]);
  });

  it("renders every term of an ONTOLOGY_ARRAY, so the array branch is not swallowed by the single-ontology branch", () => {
    const wrapper = mount(ValueEMX2, {
      props: {
        metadata: ontologyArrayColumn,
        data: [{ name: "Blood" }, { name: "Saliva" }],
      },
    });

    expect(wrapper.text()).toBe("Blood, Saliva");
  });
});
