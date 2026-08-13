import { mount } from "@vue/test-utils";
import { describe, expect, test } from "vitest";
import FilterInput from "./FilterInput.vue";
import OntologyFilter from "./OntologyFilter.vue";
import RadioFilter from "./RadioFilter.vue";
import RefListFilter from "./RefListFilter.vue";

function mountFilterInput(columnType: string, conditions: unknown[] = []) {
  // shallow: these tests assert which filter FilterInput picks and what it hands it.
  // Mounting for real also mounts InputOntology, whose `mounted` fetches ontology options
  // over the network; nothing awaits it, so every mount leaks an unhandled rejection and
  // vitest fails the run even though all assertions pass.
  return mount(FilterInput, {
    shallow: true,
    props: {
      id: "kennels",
      columnType,
      conditions,
      tableId: "Kennel",
      schemaId: "pet store",
      refLabel: "${name}",
    },
  });
}

describe("FilterInput type dispatch", () => {
  test.each(["REFBACK", "PARTS"])(
    "it should offer the reference list filter for a %s column",
    (refbackFlavor: string) => {
      const wrapper = mountFilterInput(refbackFlavor);

      expect(wrapper.findComponent(RefListFilter).exists()).toBe(true);
      expect(wrapper.getComponent(RefListFilter).props("tableId")).toBe(
        "Kennel"
      );
    }
  );
});

describe("FilterInput condition handover", () => {
  const selectedRows = [{ name: "Kennel 1" }, { name: "Kennel 2" }];

  test.each([
    "REF",
    "REF_ARRAY",
    "SELECT",
    "CHECKBOX",
    "MULTISELECT",
    "REFBACK",
    "PARTS",
  ])(
    "it should hand the whole conditions array to the single reference list filter of a %s column",
    (refType: string) => {
      const wrapper = mountFilterInput(refType, selectedRows);

      expect(wrapper.findAllComponents(RefListFilter)).toHaveLength(1);
      expect(wrapper.getComponent(RefListFilter).props("condition")).toEqual(
        selectedRows
      );
    }
  );

  test.each(["ONTOLOGY", "ONTOLOGY_ARRAY"])(
    "it should hand the whole conditions array to the single ontology filter of a %s column",
    (ontologyType: string) => {
      const wrapper = mountFilterInput(ontologyType, selectedRows);

      expect(wrapper.findAllComponents(OntologyFilter)).toHaveLength(1);
      expect(wrapper.getComponent(OntologyFilter).props("condition")).toEqual(
        selectedRows
      );
    }
  );

  test("it should give a RADIO column one filter per condition, each holding a single value", () => {
    const wrapper = mountFilterInput("RADIO", selectedRows);

    expect(wrapper.findAllComponents(RadioFilter)).toHaveLength(2);
    expect(wrapper.getComponent(RadioFilter).props("condition")).toEqual(
      selectedRows[0]
    );
  });
});
