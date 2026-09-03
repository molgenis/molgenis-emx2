import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import DisplayOntology from "../../../../app/components/display/Ontology.vue";
import CustomTooltip from "../../../../app/components/CustomTooltip.vue";
import type { IOntologyTreeItem } from "../../../../types/types";

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

// A paging or render-limit control also lives in a trailing <li>, marked
// list-none, so a row count must exclude it to count content rows only.
function contentItems(wrapper: ReturnType<typeof mount>) {
  return wrapper
    .findAll("li")
    .filter((li) => !li.classes().includes("list-none"));
}

function childListsByParentName(wrapper: ReturnType<typeof mountTree>) {
  return wrapper.findAll("li").map((item) => {
    const list = item.find("ul");
    return {
      name: item.find("span.flex").text(),
      // null: a leaf renders no child <ul> at all, collapsed or not.
      childListHidden: list.exists() ? list.classes().includes("hidden") : null,
    };
  });
}

describe("display/Ontology.vue collapse-all", () => {
  it("expands only the root when collapse-all is false, a collapsed child's list is present but hidden", () => {
    const wrapper = mountTree(false);

    expect(childListsByParentName(wrapper)).toEqual([
      { name: "Medicine", childListHidden: false },
      { name: "Cardiology", childListHidden: true },
      { name: "Pediatric cardiology", childListHidden: null },
    ]);
  });

  it("collapses every level when collapse-all is true, every child list is present but hidden", () => {
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

// Surface-dependent colouring (caret, connector, tooltip icon rest colour)
// is a pure CSS-cascade concern now: an ancestor's `.surface-inverted` class
// drives it via `--text-color-link`/`--text-color-icon-neutral`. jsdom
// computes no CSS from a stylesheet, so that is not testable here; it was
// verified in the browser instead (see the theme-sweep screenshots).
// hoverColor is the one part of this still a Vue prop, and it no longer
// varies by anything DisplayOntology exposes, so there is exactly one value
// to pin, everywhere a tooltip can render.
describe("display/Ontology.vue tooltip hover colour", () => {
  const singleItem: IOntologyTreeItem = {
    name: "Biobank",
    definition: "A collection of biological samples.",
  };
  const flatList: IOntologyTreeItem[] = [
    { name: "Genomics", definition: "Study of genomes" },
    { name: "Proteomics", definition: "Study of proteins" },
  ];
  const nestedWithDefinition: IOntologyTreeItem[] = [
    {
      name: "Pediatric cardiology",
      definition: "Heart care for children",
      parent: cardiology,
    },
  ];

  it("uses hoverColor white for the single-item branch", () => {
    const wrapper = mount(DisplayOntology, { props: { value: singleItem } });
    expect(wrapper.findComponent(CustomTooltip).props("hoverColor")).toBe(
      "white"
    );
  });

  it("uses hoverColor white for the flat-list branch", () => {
    const wrapper = mount(DisplayOntology, { props: { value: flatList } });
    const tooltips = wrapper.findAllComponents(CustomTooltip);
    expect(tooltips.every((t) => t.props("hoverColor") === "white")).toBe(true);
  });

  it("uses hoverColor white for a non-root tree row", () => {
    const wrapper = mount(DisplayOntology, {
      props: { value: nestedWithDefinition, collapseAll: false },
    });
    expect(wrapper.findComponent(CustomTooltip).props("hoverColor")).toBe(
      "white"
    );
  });
});

describe("display/Ontology.vue renders every root", () => {
  it("renders more than ten roots without truncating or hiding any of them", () => {
    const manyRoots: IOntologyTreeItem[] = Array.from(
      { length: 15 },
      (_, i) => ({ name: `Root ${i}` })
    );
    const wrapper = mount(DisplayOntology, {
      props: { value: manyRoots },
    });
    const items = wrapper.findAll("li");
    expect(items).toHaveLength(15);
    expect(items.every((li) => !li.classes().includes("hidden"))).toBe(true);
    expect(wrapper.text()).not.toContain("Show");
  });
});

describe("display/Ontology.vue name affordance", () => {
  it("gives a leaf's name span no pointer-cursor class", () => {
    const wrapper = mountTree(true);
    const leafSpan = wrapper.findAll("li").at(-1)!.find("span.flex");
    expect(leafSpan.classes()).not.toContain("hover:cursor-pointer");
  });

  it("still toggles a branch's child list when its name span is clicked", async () => {
    const wrapper = mountTree(true);
    const rootItem = wrapper.findAll("li")[0];
    expect(rootItem.find("ul").classes()).toContain("hidden");

    await rootItem.find("span.flex").trigger("click");

    expect(rootItem.find("ul").classes()).not.toContain("hidden");
  });
});

describe("display/Ontology.vue root markup", () => {
  it("renders the flat list with no wrapper element around the ul", () => {
    const wrapper = mount(DisplayOntology, {
      props: { value: [{ name: "Genomics" }, { name: "Proteomics" }] },
    });
    expect(wrapper.element.tagName).toBe("UL");
  });

  it("renders the single item as OntologyRow's own root, no extra wrapper around it", () => {
    const wrapper = mount(DisplayOntology, {
      props: { value: { name: "Biobank" } },
    });
    // OntologyRow's own root is a <div>; a <span> here would nest a <div>
    // inside a <span>, invalid HTML, and be one wrapper too many.
    expect(wrapper.element.tagName).toBe("DIV");
    expect(wrapper.element.classList.contains("flex")).toBe(true);
  });
});

describe("display/Ontology.vue root-level item paging", () => {
  const manyRoots: IOntologyTreeItem[] = Array.from({ length: 12 }, (_, i) => ({
    name: `Root ${i}`,
  }));

  function mountPaged(maxItems: number, itemStep: number) {
    return mount(DisplayOntology, {
      props: { value: manyRoots, maxItems, itemStep },
    });
  }

  it("shows maxItems rows and hides the rest without removing them from the DOM", () => {
    const wrapper = mountPaged(5, 3);
    const items = contentItems(wrapper);
    expect(items).toHaveLength(12);
    expect(
      items.slice(0, 5).every((li) => !li.classes().includes("hidden"))
    ).toBe(true);
    expect(items.slice(5).every((li) => li.classes().includes("hidden"))).toBe(
      true
    );
  });

  it("reveals itemStep more rows on show more, and stays incomplete", async () => {
    const wrapper = mountPaged(5, 3);
    await wrapper.find("button").trigger("click");

    const items = contentItems(wrapper);
    expect(
      items.slice(0, 8).every((li) => !li.classes().includes("hidden"))
    ).toBe(true);
    expect(items.slice(8).every((li) => li.classes().includes("hidden"))).toBe(
      true
    );
    expect(wrapper.find("button").text()).toBe("Show more");
  });

  it("flips to show less once the level is exhausted, and resets to maxItems on click", async () => {
    const wrapper = mountPaged(5, 3);
    await wrapper.find("button").trigger("click"); // 5 -> 8
    await wrapper.find("button").trigger("click"); // 8 -> 11
    await wrapper.find("button").trigger("click"); // 11 -> 12, fully expanded

    expect(wrapper.find("button").text()).toBe("Show less");
    expect(
      contentItems(wrapper).every((li) => !li.classes().includes("hidden"))
    ).toBe(true);

    await wrapper.find("button").trigger("click"); // reset

    const items = contentItems(wrapper);
    expect(wrapper.find("button").text()).toBe("Show more");
    expect(
      items.slice(0, 5).every((li) => !li.classes().includes("hidden"))
    ).toBe(true);
    expect(items.slice(5).every((li) => li.classes().includes("hidden"))).toBe(
      true
    );
  });
});

describe("display/Ontology.vue two levels page independently", () => {
  function buildTwoBranches(): IOntologyTreeItem[] {
    const groupA: IOntologyTreeItem = { name: "Group A" };
    const groupB: IOntologyTreeItem = { name: "Group B" };
    const childrenOf = (parent: IOntologyTreeItem, prefix: string) =>
      Array.from({ length: 6 }, (_, i) => ({
        name: `${prefix}-item-${i}`,
        parent,
      }));
    return [...childrenOf(groupA, "A"), ...childrenOf(groupB, "B")];
  }

  function hiddenFlags(
    wrapper: ReturnType<typeof mount>,
    prefix: string
  ): boolean[] {
    return wrapper
      .findAll("li")
      .filter((li) => li.find("span.flex").text().startsWith(prefix))
      .map((li) => li.classes().includes("hidden"));
  }

  it("paging one branch's children leaves the sibling branch's paging untouched", async () => {
    const wrapper = mount(DisplayOntology, {
      props: {
        value: buildTwoBranches(),
        collapseAll: false,
        maxItems: 3,
        itemStep: 2,
      },
    });

    expect(hiddenFlags(wrapper, "A-item")).toEqual([
      false,
      false,
      false,
      true,
      true,
      true,
    ]);
    expect(hiddenFlags(wrapper, "B-item")).toEqual([
      false,
      false,
      false,
      true,
      true,
      true,
    ]);

    const showMoreButtons = wrapper
      .findAll("button")
      .filter((b) => b.text() === "Show more");
    expect(showMoreButtons).toHaveLength(2);

    await showMoreButtons[0]!.trigger("click");

    expect(hiddenFlags(wrapper, "A-item")).toEqual([
      false,
      false,
      false,
      false,
      false,
      true,
    ]);
    expect(hiddenFlags(wrapper, "B-item")).toEqual([
      false,
      false,
      false,
      true,
      true,
      true,
    ]);
  });
});

describe("display/Ontology.vue renderLimit safety valve", () => {
  function buildFlatRoots(n: number): IOntologyTreeItem[] {
    return Array.from({ length: n }, (_, i) => ({ name: `Node ${i}` }));
  }

  it("caps total rendered nodes at renderLimit; the rest is genuinely absent from the DOM", () => {
    const wrapper = mount(DisplayOntology, {
      props: { value: buildFlatRoots(10), renderLimit: 4 },
    });
    expect(contentItems(wrapper)).toHaveLength(4);
    expect(wrapper.find("button").text()).toBe("Load more");
  });

  it("renderMore extends the budget by renderLimit and renders the rest", async () => {
    const wrapper = mount(DisplayOntology, {
      props: { value: buildFlatRoots(10), renderLimit: 4 },
    });

    await wrapper.find("button").trigger("click");
    expect(contentItems(wrapper)).toHaveLength(8);

    await wrapper.find("button").trigger("click");
    expect(contentItems(wrapper)).toHaveLength(10);
    expect(wrapper.find("button").exists()).toBe(false);
  });
});
