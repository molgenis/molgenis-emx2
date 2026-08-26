import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import CustomTooltip from "../../../../app/components/CustomTooltip.vue";
import ValueObject from "../../../../app/components/value/Object.vue";
import type { IColumn } from "../../../../../metadata-utils/src/types";

const ontology: IColumn = {
  id: "disease",
  label: "Disease",
  columnType: "ONTOLOGY",
};

const term = {
  order: 3,
  name: "Diabetes",
  code: "E11",
  definition: "A metabolic disease that causes high blood sugar.",
  ontologyTermURI: "http://purl.obolibrary.org/obo/MONDO_0005015",
};

describe("value/Object.vue", () => {
  it("renders the name when there is no refLabel template", () => {
    const wrapper = mount(ValueObject, {
      props: { metadata: ontology, data: term },
    });

    expect(wrapper.find(".text-link").text().trim()).toBe("Diabetes");
  });

  it("still prefers an explicit refLabel template over the name", () => {
    const wrapper = mount(ValueObject, {
      props: {
        metadata: { ...ontology, refLabel: "${code}" },
        data: term,
      },
    });

    expect(wrapper.find(".text-link").text().trim()).toBe("E11");
  });

  it("offers the definition behind a tooltip when the term has one", () => {
    const wrapper = mount(ValueObject, {
      props: { metadata: ontology, data: term },
    });

    const tooltip = wrapper.findComponent(CustomTooltip);
    expect(tooltip.exists()).toBe(true);
    expect(tooltip.props("content")).toBe(
      "A metabolic disease that causes high blood sugar."
    );
  });

  it("shows no tooltip when the term has no definition", () => {
    const { definition, ...withoutDefinition } = term;
    const wrapper = mount(ValueObject, {
      props: { metadata: ontology, data: withoutDefinition },
    });

    expect(wrapper.findComponent(CustomTooltip).exists()).toBe(false);
  });

  it("joins the remaining fields with a space when there is no name", () => {
    const wrapper = mount(ValueObject, {
      props: {
        metadata: { id: "ref", label: "Ref", columnType: "REF" },
        data: { firstName: "Ada", lastName: "Lovelace" },
      },
    });

    expect(wrapper.find(".text-link").text().trim()).toBe("Ada Lovelace");
  });
});
