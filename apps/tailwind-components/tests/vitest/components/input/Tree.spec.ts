import { describe, it, expect, vi } from "vitest";
import { mount, flushPromises } from "@vue/test-utils";
import { nextTick } from "vue";
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
  describe("auto-expand on small trees", () => {
    it("auto-expands all parent nodes when tree has ≤25 total nodes", () => {
      const wrapper = mount(Tree, {
        props: {
          id: "test-tree",
          nodes: makeSmallHierarchicalNodes(),
          modelValue: [],
        },
      });

      const expandButtons = wrapper.findAll("button[aria-controls]");
      expect(expandButtons.length).toBeGreaterThan(0);
      expandButtons.forEach((btn) => {
        expect(btn.attributes("aria-expanded")).toBe("true");
      });

      expect(wrapper.text()).toContain("Child 1A");
      expect(wrapper.text()).toContain("Child 2B");
      expect(wrapper.text()).toContain("Child 3A");
    });

    it("does not auto-expand when tree has >25 total nodes", () => {
      const wrapper = mount(Tree, {
        props: {
          id: "test-tree",
          nodes: makeLargeHierarchicalNodes(),
          modelValue: [],
        },
      });

      const expandButtons = wrapper.findAll("button[aria-controls]");
      expect(expandButtons.length).toBeGreaterThan(0);
      expandButtons.forEach((btn) => {
        expect(btn.attributes("aria-expanded")).toBe("false");
      });

      expect(wrapper.text()).not.toContain("Child 1A");
    });
  });

  describe("disableInternalSearch prop", () => {
    it("renders internal search by default when >25 nodes", () => {
      const wrapper = mount(Tree, {
        props: {
          id: "test-tree",
          nodes: makeLargeHierarchicalNodes(),
          modelValue: [],
        },
      });
      expect(
        wrapper.find('[id="test-tree-tree-search-input-container"]').exists()
      ).toBe(true);
    });

    it("renders internal search by default when tree has children", () => {
      const wrapper = mount(Tree, {
        props: {
          id: "test-tree",
          nodes: makeSmallHierarchicalNodes(),
          modelValue: [],
        },
      });
      expect(
        wrapper.find('[id="test-tree-tree-search-input-container"]').exists()
      ).toBe(true);
    });

    it("suppresses internal search when disableInternalSearch is true, even with >25 nodes", () => {
      const wrapper = mount(Tree, {
        props: {
          id: "test-tree",
          nodes: makeLargeHierarchicalNodes(),
          modelValue: [],
          disableInternalSearch: true,
        },
      });
      expect(
        wrapper.find('[id="test-tree-tree-search-input-container"]').exists()
      ).toBe(false);
    });

    it("suppresses internal search when disableInternalSearch is true, even with children", () => {
      const wrapper = mount(Tree, {
        props: {
          id: "test-tree",
          nodes: makeSmallHierarchicalNodes(),
          modelValue: [],
          disableInternalSearch: true,
        },
      });
      expect(
        wrapper.find('[id="test-tree-tree-search-input-container"]').exists()
      ).toBe(false);
    });
  });

  describe("preserve expand state across node rebuilds", () => {
    it("preserves expand state when nodes prop changes", async () => {
      const initialNodes = makeSmallHierarchicalNodes();
      const wrapper = mount(Tree, {
        props: {
          id: "test-tree",
          nodes: initialNodes,
          modelValue: [],
        },
      });

      const parent1Btn = wrapper.find("button[aria-controls='parent1']");
      expect(parent1Btn.attributes("aria-expanded")).toBe("true");

      await parent1Btn.trigger("click");
      await nextTick();

      expect(
        wrapper
          .find("button[aria-controls='parent1']")
          .attributes("aria-expanded")
      ).toBe("false");

      const updatedNodes: ITreeNode[] = initialNodes.map((node) => ({
        ...node,
        label: node.label + " (updated)",
        children: node.children?.map((child) => ({
          ...child,
          label: child.label + " (updated)",
        })),
      }));

      await wrapper.setProps({ nodes: updatedNodes });
      await flushPromises();
      await nextTick();

      expect(
        wrapper
          .find("button[aria-controls='parent1']")
          .attributes("aria-expanded")
      ).toBe("false");

      expect(
        wrapper
          .find("button[aria-controls='parent2']")
          .attributes("aria-expanded")
      ).toBe("true");
    });
  });
});
