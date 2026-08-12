import { mount } from "@vue/test-utils";
import { describe, expect, test } from "vitest";
import DataDisplayCell from "./DataDisplayCell.vue";
import ListDisplay from "./cellTypes/ListDisplay.vue";
import ObjectDisplay from "./cellTypes/ObjectDisplay.vue";

function mountCell(columnType: string) {
  return mount(DataDisplayCell, {
    props: {
      data: [{ name: "Kennel 1" }, { name: "Kennel 2" }],
      metadata: {
        id: "kennels",
        columnType,
        refTableId: "Kennel",
        refLabel: "${name}",
      },
    },
  });
}

describe("DataDisplayCell type dispatch", () => {
  test.each([
    "REFBACK",
    "PARTS",
    "REF_ARRAY",
    "CHECKBOX",
    "MULTISELECT",
    "ONTOLOGY_ARRAY",
  ])(
    "it should display a %s value as a list of referenced objects",
    (multiValuedRefType: string) => {
      const wrapper = mountCell(multiValuedRefType);

      expect(wrapper.findComponent(ListDisplay).exists()).toBe(true);
      expect(wrapper.findAllComponents(ObjectDisplay)).toHaveLength(2);
      expect(wrapper.text()).toContain("Kennel 1");
    }
  );

  test.each(["REF", "SELECT", "RADIO", "ONTOLOGY"])(
    "it should display a single %s value as one referenced object, never as a list",
    (singleRefType: string) => {
      const wrapper = mount(DataDisplayCell, {
        props: {
          data: { name: "Kennel 1" },
          metadata: {
            id: "kennel",
            columnType: singleRefType,
            refTableId: "Kennel",
            refLabel: "${name}",
          },
        },
      });

      expect(wrapper.findComponent(ListDisplay).exists()).toBe(false);
      expect(wrapper.findAllComponents(ObjectDisplay)).toHaveLength(1);
      expect(wrapper.text()).toContain("Kennel 1");
    }
  );
});
