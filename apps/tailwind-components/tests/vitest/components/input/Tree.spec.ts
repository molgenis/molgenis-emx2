import { describe, it, expect, vi } from "vitest";
import { mount } from "@vue/test-utils";
import Tree from "../../../../app/components/input/Tree.vue";
import type { ITreeNode } from "../../../../types/types";

const createMockObserver = () => ({
  observe: vi.fn(),
  unobserve: vi.fn(),
  disconnect: vi.fn(),
  takeRecords: vi.fn(() => []),
});

global.IntersectionObserver = vi
  .fn()
  .mockImplementation(() => createMockObserver());

function makeSmallHierarchicalNodes(): ITreeNode[] {
  return [
    {
      name: "parent1",
      label: "Parent 1",
      children: [
        { name: "child1a", label: "Child 1A" },
        { name: "child1b", label: "Child 1B" },
      ],
    },
    {
      name: "parent2",
      label: "Parent 2",
      children: [
        { name: "child2a", label: "Child 2A" },
        { name: "child2b", label: "Child 2B" },
      ],
    },
    {
      name: "parent3",
      label: "Parent 3",
      children: [
        { name: "child3a", label: "Child 3A" },
        { name: "child3b", label: "Child 3B" },
      ],
    },
  ];
}

function makeLargeHierarchicalNodes(): ITreeNode[] {
  return Array.from({ length: 13 }, (_, parentIndex) => ({
    name: `parent${parentIndex + 1}`,
    label: `Parent ${parentIndex + 1}`,
    children: [
      { name: `child${parentIndex + 1}a`, label: `Child ${parentIndex + 1}A` },
    ],
  }));
}

describe("Tree", () => {
  describe("renders tree nodes", () => {
    it("renders root nodes by name", () => {
      const wrapper = mount(Tree, {
        props: {
          id: "test-tree",
          nodes: makeSmallHierarchicalNodes(),
          modelValue: [],
        },
      });

      expect(wrapper.text()).toContain("parent1");
      expect(wrapper.text()).toContain("parent2");
      expect(wrapper.text()).toContain("parent3");
    });

    it("renders a toggle button for parent nodes", () => {
      const wrapper = mount(Tree, {
        props: {
          id: "test-tree",
          nodes: makeSmallHierarchicalNodes(),
          modelValue: [],
        },
      });

      const expandButtons = wrapper.findAll("button[aria-controls]");
      expect(expandButtons.length).toBeGreaterThan(0);
    });

    it("renders search toggle button by default", () => {
      const wrapper = mount(Tree, {
        props: {
          id: "test-tree",
          nodes: makeSmallHierarchicalNodes(),
          modelValue: [],
        },
      });
      expect(wrapper.find("button").exists()).toBe(true);
    });
  });

  describe("selection", () => {
    it("emits update:modelValue when a node checkbox is clicked", async () => {
      const wrapper = mount(Tree, {
        props: {
          id: "test-tree",
          nodes: [
            { name: "a", label: "A" },
            { name: "b", label: "B" },
          ],
          modelValue: [],
        },
      });

      const checkbox = wrapper.find('input[id="test-tree-a-input"]');
      await checkbox.trigger("click");
      const emitted = wrapper.emitted("update:modelValue");
      expect(emitted).toBeTruthy();
      expect(emitted![0][0]).toContain("a");
    });

    it("reflects selected state from modelValue", () => {
      const wrapper = mount(Tree, {
        props: {
          id: "test-tree",
          nodes: [
            { name: "a", label: "A" },
            { name: "b", label: "B" },
          ],
          modelValue: ["a"],
        },
      });

      const checkbox = wrapper.find('input[id="test-tree-a-input"]');
      expect((checkbox.element as HTMLInputElement).checked).toBe(true);
    });
  });
});
