import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import ContentTypeOntologyArray from "../../../../app/components/content/type/ContentTypeOntologyArray.vue";
import type {
  columnValue,
  IColumn,
} from "../../../../../metadata-utils/src/types";

const metadata: IColumn = {
  id: "diagnosis",
  label: "Diagnosis",
  columnType: "ONTOLOGY_ARRAY",
};

function mountWithValue(value: columnValue) {
  return mount(ContentTypeOntologyArray, {
    props: { field: { meta: metadata, value } },
  });
}

describe("content/type/ContentTypeOntologyArray.vue", () => {
  it("joins the ontology terms with a comma", () => {
    const wrapper = mountWithValue([{ name: "Asthma" }, { name: "Diabetes" }]);

    expect(wrapper.text()).toBe("Asthma, Diabetes");
  });

  it("shows the label of a term in preference to its name", () => {
    const wrapper = mountWithValue([
      { name: "asthma", label: "Asthma (chronic)" },
      { name: "diabetes" },
    ]);

    expect(wrapper.text()).toBe("Asthma (chronic), diabetes");
  });

  it("leaves out terms that have no display text", () => {
    const wrapper = mountWithValue([
      { name: "Asthma" },
      {},
      { name: "" },
      { name: "Diabetes" },
    ]);

    expect(wrapper.text()).toBe("Asthma, Diabetes");
  });

  it.each<[string, columnValue]>([
    ["a plain string", "Asthma"],
    ["a single object", { name: "Asthma" }],
    ["a number", 42],
    ["null", null],
    ["undefined", undefined],
  ])("renders nothing when the value is %s", (_description, value) => {
    const wrapper = mountWithValue(value);

    expect(wrapper.text()).toBe("");
  });
});
