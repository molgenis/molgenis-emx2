import { flushPromises, mount } from "@vue/test-utils";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { IColumn } from "../../../../../metadata-utils/src/types";
import ValueOntology from "../../../../app/components/value/Ontology.vue";

vi.mock("../../../../app/composables/fetchGraphql", () => ({
  default: vi.fn(),
}));

import fetchGraphql from "../../../../app/composables/fetchGraphql";

beforeEach(() => {
  vi.clearAllMocks();
});

const metadata: IColumn = {
  id: "diagnosis",
  label: "Diagnosis",
  columnType: "ONTOLOGY",
  refSchemaId: "OntologySchema",
  refTableId: "Diagnoses",
};

describe("value/Ontology.vue", () => {
  it("renders the resolved ancestor above the record's own term", async () => {
    vi.mocked(fetchGraphql).mockResolvedValueOnce({
      Diagnoses: [
        { name: "Cardiology", parent: { name: "Medicine" } },
        { name: "Medicine", parent: null },
      ],
    });

    const wrapper = mount(ValueOntology, {
      props: { metadata, data: { name: "Cardiology" } },
    });
    await flushPromises();

    const names = wrapper.findAll("li span.flex").map((span) => span.text());
    expect(names).toEqual(["Medicine", "Cardiology"]);
  });

  it("renders the term as-is when the column carries no ref table", async () => {
    const noRefMetadata: IColumn = {
      id: "diagnosis",
      label: "Diagnosis",
      columnType: "ONTOLOGY",
    };

    const wrapper = mount(ValueOntology, {
      props: { metadata: noRefMetadata, data: { name: "Cardiology" } },
    });
    await flushPromises();

    expect(fetchGraphql).not.toHaveBeenCalled();
    expect(wrapper.text()).toContain("Cardiology");
  });
});
