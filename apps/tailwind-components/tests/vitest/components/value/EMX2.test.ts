import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import EMX2Value from "../../../../app/components/value/EMX2.vue";
import ValueList from "../../../../app/components/value/List.vue";
import type { IColumn } from "../../../../../metadata-utils/src/types";

const ontologyArray: IColumn = {
  id: "diseases",
  label: "Diseases",
  columnType: "ONTOLOGY_ARRAY",
};

const singleString: IColumn = {
  id: "name",
  label: "Name",
  columnType: "STRING",
};

const eightTerms = Array.from({ length: 8 }, (_, index) => ({
  name: `Term ${index + 1}`,
}));

describe("value/EMX2.vue", () => {
  it("bounds a multi-valued column by lines, not by item count", () => {
    const wrapper = mount(EMX2Value, {
      props: { metadata: ontologyArray, data: eightTerms },
    });

    expect(wrapper.findComponent(ValueList).props("maxLines")).toBe(3);

    const overridden = mount(EMX2Value, {
      props: { metadata: ontologyArray, data: eightTerms, maxLines: 8 },
    });

    expect(overridden.findComponent(ValueList).props("maxLines")).toBe(8);
  });

  it("routes a single-valued column past the list entirely", () => {
    const wrapper = mount(EMX2Value, {
      props: { metadata: singleString, data: "Alice" },
    });

    expect(wrapper.findComponent(ValueList).exists()).toBe(false);
  });

  it("leaves the bound off entirely when the caller bounds the value itself", () => {
    const wrapper = mount(EMX2Value, {
      props: { metadata: ontologyArray, data: eightTerms, collapse: false },
    });

    expect(wrapper.findComponent(ValueList).props("maxLines")).toBe(undefined);
  });

  it("bounds CHECKBOX and MULTISELECT, which are multi-valued without an _ARRAY suffix", () => {
    for (const columnType of ["CHECKBOX", "MULTISELECT"] as const) {
      const wrapper = mount(EMX2Value, {
        props: {
          metadata: { id: "options", label: "Options", columnType },
          data: eightTerms,
        },
      });

      expect(wrapper.findComponent(ValueList).props("maxLines")).toBe(3);
    }
  });
});
