import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import DisplayOntology from "../../../../app/components/display/Ontology.vue";
import CustomTooltip from "../../../../app/components/CustomTooltip.vue";
import type { IOntologyTreeItem } from "../../../../app/utils/buildOntologyTree";

const medicine: IOntologyTreeItem = { name: "Medicine" };
const cardiology: IOntologyTreeItem = { name: "Cardiology", parent: medicine };
const threeLevelTerms: IOntologyTreeItem[] = [
  { name: "Pediatric cardiology", parent: cardiology },
];

function mountTree(collapseAll: boolean) {
  return mount(DisplayOntology, {
    props: { value: threeLevelTerms, collapseAll },
  });
}

function childListsByParentName(wrapper: ReturnType<typeof mountTree>) {
  return wrapper.findAll("li").map((item) => ({
    name: item.find("span.flex").text(),
    childListHidden: item.find("ul").exists()
      ? item.find("ul").classes().includes("hidden")
      : null,
  }));
}

describe("display/Ontology.vue collapse-all", () => {
  it("expands only the root when collapse-all is false, deeper levels stay collapsed", () => {
    const wrapper = mountTree(false);

    expect(childListsByParentName(wrapper)).toEqual([
      { name: "Medicine", childListHidden: false },
      { name: "Cardiology", childListHidden: true },
      { name: "Pediatric cardiology", childListHidden: null },
    ]);
  });

  it("collapses every level when collapse-all is true", () => {
    const wrapper = mountTree(true);

    expect(childListsByParentName(wrapper)).toEqual([
      { name: "Medicine", childListHidden: true },
      { name: "Cardiology", childListHidden: true },
      { name: "Pediatric cardiology", childListHidden: null },
    ]);
  });
});

describe("display/Ontology.vue expand control accessibility", () => {
  it("sets aria-expanded to false on the expand control when collapsed", () => {
    const wrapper = mountTree(true);
    const button = wrapper.find("button");
    expect(button.attributes("aria-expanded")).toBe("false");
  });

  it("sets aria-expanded to true on the expand control when expanded", () => {
    const wrapper = mountTree(false);
    const button = wrapper.find("button");
    expect(button.attributes("aria-expanded")).toBe("true");
  });
});

describe("display/Ontology.vue inverted", () => {
  const singleItem: IOntologyTreeItem = {
    name: "Biobank",
    definition: "A collection of biological samples.",
  };
  const flatList: IOntologyTreeItem[] = [
    { name: "Genomics", definition: "Study of genomes" },
    { name: "Proteomics", definition: "Study of proteins" },
  ];

  it("uses hoverColor white for the single-item branch by default", () => {
    const wrapper = mount(DisplayOntology, {
      props: { value: singleItem },
    });
    expect(wrapper.findComponent(CustomTooltip).props("hoverColor")).toBe(
      "white"
    );
  });

  it("uses hoverColor none for the single-item branch when inverted", () => {
    const wrapper = mount(DisplayOntology, {
      props: { value: singleItem, inverted: true },
    });
    expect(wrapper.findComponent(CustomTooltip).props("hoverColor")).toBe(
      "none"
    );
  });

  it("uses hoverColor white for the flat-list branch by default", () => {
    const wrapper = mount(DisplayOntology, {
      props: { value: flatList },
    });
    const tooltips = wrapper.findAllComponents(CustomTooltip);
    expect(tooltips.every((t) => t.props("hoverColor") === "white")).toBe(true);
  });

  it("uses hoverColor none for the flat-list branch when inverted", () => {
    const wrapper = mount(DisplayOntology, {
      props: { value: flatList, inverted: true },
    });
    const tooltips = wrapper.findAllComponents(CustomTooltip);
    expect(tooltips.every((t) => t.props("hoverColor") === "none")).toBe(true);
  });
});

describe("display/Ontology.vue renders every root", () => {
  it("renders more than ten roots without truncating any of them", () => {
    const manyRoots: IOntologyTreeItem[] = Array.from(
      { length: 15 },
      (_, i) => ({ name: `Root ${i}` })
    );
    const wrapper = mount(DisplayOntology, {
      props: { value: manyRoots },
    });
    expect(wrapper.findAll("li")).toHaveLength(15);
    expect(wrapper.text()).not.toContain("Show");
  });
});

describe("display/Ontology.vue root markup", () => {
  it("renders the flat list with no wrapper element around the ul", () => {
    const wrapper = mount(DisplayOntology, {
      props: { value: [{ name: "Genomics" }, { name: "Proteomics" }] },
    });
    expect(wrapper.element.tagName).toBe("UL");
  });

  it("renders the single item with no wrapper element around the span", () => {
    const wrapper = mount(DisplayOntology, {
      props: { value: { name: "Biobank" } },
    });
    expect(wrapper.element.tagName).toBe("SPAN");
  });
});
