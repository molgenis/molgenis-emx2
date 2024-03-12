import { describe, it, expect } from "vitest";
import { mount } from "@vue/test-utils";

import FilterTree from "@/components/filter/FilterTree.vue";
import type { ITreeNode } from "~/types/types";

function generateTreeData(width: number, depth: number, parentName?: string) {
  const nodes = [];
  for (let i = 0; i < width; i++) {
    const name = parentName ? parentName + `.${i}` : `Node ${i}`;
    const node: ITreeNode = {
      name,
      selected: false,
      expanded: false,
      children: depth > 0 ? generateTreeData(width, depth - 1, name) : [],
    };

    nodes.push(node);
  }
  return nodes;
}

describe("FilterTree", () => {
  it("show top level nodes as list by default ( tree is collapes)", () => {
    const width = 2;
    const depth = 4;
    const rootNodes = generateTreeData(width, depth);
    const wrapper = mount(FilterTree, {
      props: {
        rootNodes,
        modelValue: [],
      },
    });
    expect(wrapper.find("ul").exists()).toBe(true);
    // 3 ul 's for depth of 4, but we only get 3 (and a <treechild> stub) because of shallow mount, stange
    expect(wrapper.find("ul > li > ul > li > ul ").exists()).toBe(true);
    expect(wrapper.findAll("ul:first-child > li").length).toEqual(width);

    // now try using a snapshot
    expect(wrapper.element).toMatchSnapshot();
  });
});
