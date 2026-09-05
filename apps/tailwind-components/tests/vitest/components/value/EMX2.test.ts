import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import EMX2Value from "../../../../app/components/value/EMX2.vue";
import ValueList from "../../../../app/components/value/List.vue";
import ValueOntology from "../../../../app/components/value/Ontology.vue";
import ValueText from "../../../../app/components/value/Text.vue";
import type { IColumn } from "../../../../../metadata-utils/src/types";

const ontologyArray: IColumn = {
  id: "diseases",
  label: "Diseases",
  columnType: "ONTOLOGY_ARRAY",
};

const ontology: IColumn = {
  id: "disease",
  label: "Disease",
  columnType: "ONTOLOGY",
};

const stringArray: IColumn = {
  id: "aliases",
  label: "Aliases",
  columnType: "STRING_ARRAY",
};

const singleString: IColumn = {
  id: "name",
  label: "Name",
  columnType: "STRING",
};

const singleText: IColumn = {
  id: "description",
  label: "Description",
  columnType: "TEXT",
};

const eightTerms = Array.from({ length: 8 }, (_, index) => ({
  name: `Term ${index + 1}`,
}));

const eightStrings = Array.from(
  { length: 8 },
  (_, index) => `Term ${index + 1}`
);

describe("value/EMX2.vue", () => {
  it("bounds a multi-valued column by lines, not by item count", () => {
    const wrapper = mount(EMX2Value, {
      props: { metadata: stringArray, data: eightStrings },
    });

    expect(wrapper.findComponent(ValueList).props("maxLines")).toBe(undefined);

    const overridden = mount(EMX2Value, {
      props: { metadata: stringArray, data: eightStrings, maxLines: 8 },
    });

    expect(overridden.findComponent(ValueList).props("maxLines")).toBe(8);
  });

  it("bounds a TEXT value the way it bounds a list", () => {
    const wrapper = mount(EMX2Value, {
      props: { metadata: singleText, data: "A long description." },
    });

    expect(wrapper.findComponent(ValueText).props("maxLines")).toBe(undefined);

    const uncollapsed = mount(EMX2Value, {
      props: {
        metadata: singleText,
        data: "A long description.",
        truncate: false,
      },
    });

    expect(uncollapsed.findComponent(ValueText).props("truncate")).toBe(false);
  });

  it("routes a refback through the list, so it is bounded like any other", () => {
    const refback: IColumn = {
      id: "visits",
      label: "Visits",
      columnType: "REFBACK",
      refSchemaId: "pet store",
      refTableId: "Visit",
      refLabel: "${date}",
    };
    const wrapper = mount(EMX2Value, {
      props: { metadata: refback, data: [{ date: "2023-01-10" }] },
    });

    expect(wrapper.findComponent(ValueList).exists()).toBe(true);
    expect(wrapper.findComponent(ValueList).props("maxLines")).toBe(undefined);
  });

  it("routes a single-valued column past the list entirely", () => {
    const wrapper = mount(EMX2Value, {
      props: { metadata: singleString, data: "Alice" },
    });

    expect(wrapper.findComponent(ValueList).exists()).toBe(false);
  });

  it("leaves the bound off entirely when the caller bounds the value itself", () => {
    const wrapper = mount(EMX2Value, {
      props: { metadata: stringArray, data: eightStrings, truncate: false },
    });

    expect(wrapper.findComponent(ValueList).props("truncate")).toBe(false);
  });

  it("routes ONTOLOGY and ONTOLOGY_ARRAY through the ontology tree, not the list", () => {
    const single = mount(EMX2Value, {
      props: { metadata: ontology, data: { name: "Term 1" } },
    });

    expect(single.findComponent(ValueOntology).exists()).toBe(true);
    expect(single.findComponent(ValueList).exists()).toBe(false);

    const array = mount(EMX2Value, {
      props: { metadata: ontologyArray, data: eightTerms },
    });

    expect(array.findComponent(ValueOntology).exists()).toBe(true);
    expect(array.findComponent(ValueList).exists()).toBe(false);
  });

  it("bounds CHECKBOX and MULTISELECT, which are multi-valued without an _ARRAY suffix", () => {
    for (const columnType of ["CHECKBOX", "MULTISELECT"] as const) {
      const wrapper = mount(EMX2Value, {
        props: {
          metadata: { id: "options", label: "Options", columnType },
          data: eightTerms,
        },
      });

      expect(wrapper.findComponent(ValueList).props("maxLines")).toBe(
        undefined
      );
    }
  });
});
