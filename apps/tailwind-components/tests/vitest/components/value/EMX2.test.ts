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
  it("caps a stored multi-valued column at five items by default", () => {
    const wrapper = mount(EMX2Value, {
      props: { metadata: ontologyArray, data: eightTerms },
    });

    expect(wrapper.findComponent(ValueList).props("maxItems")).toBe(5);
  });

  it("lets the caller override the cap", () => {
    const wrapper = mount(EMX2Value, {
      props: { metadata: ontologyArray, data: eightTerms, maxItems: 2 },
    });

    expect(wrapper.findComponent(ValueList).props("maxItems")).toBe(2);
  });

  it("leaves the cap undefined for a single-valued column", () => {
    const wrapper = mount(EMX2Value, {
      props: { metadata: singleString, data: "Alice" },
    });

    expect(wrapper.findComponent(ValueList).exists()).toBe(false);
  });

  it("caps CHECKBOX and MULTISELECT, which are stored multi-valued without an _ARRAY suffix", () => {
    for (const columnType of ["CHECKBOX", "MULTISELECT"] as const) {
      const wrapper = mount(EMX2Value, {
        props: {
          metadata: { id: "options", label: "Options", columnType },
          data: eightTerms,
        },
      });

      expect(wrapper.findComponent(ValueList).props("maxItems")).toBe(5);
    }
  });
});
