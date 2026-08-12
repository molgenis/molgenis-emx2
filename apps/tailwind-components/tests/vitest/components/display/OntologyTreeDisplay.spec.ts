import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import OntologyTreeDisplay from "../../../../app/components/display/OntologyTreeDisplay.vue";
import type { IOntologyTreeItem } from "../../../../app/utils/buildOntologyTree";

const medicine: IOntologyTreeItem = { name: "Medicine" };
const cardiology: IOntologyTreeItem = { name: "Cardiology", parent: medicine };
const threeLevelTerms: IOntologyTreeItem[] = [
  { name: "Pediatric cardiology", parent: cardiology },
];

function mountTree(collapseAll: boolean) {
  return mount(OntologyTreeDisplay, {
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

describe("display/OntologyTreeDisplay.vue collapse-all", () => {
  it("expands every level, not just the root, when collapse-all is false", () => {
    const wrapper = mountTree(false);

    expect(childListsByParentName(wrapper)).toEqual([
      { name: "Medicine", childListHidden: false },
      { name: "Cardiology", childListHidden: false },
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
