import type { ITreeNode } from "~/types/types";
import TreeFilter from "./TreeFilter.vue";
import type { Meta, StoryObj } from "@storybook/vue3";

function generateTreeData(width: number, depth: number) {
  const nodes = [];
  for (let i = 0; i < width; i++) {
    const node: ITreeNode = {
      name: `Node ${i}`,
      selected: "none",
      expanded: false,
      children: depth > 0 ? generateTreeData(width, depth - 1) : [],
    };

    nodes.push(node);
  }
  return nodes;
}

const meta = {
  title: "fiter/TreeFilter",
  component: TreeFilter,
};

export default meta;
type Story = StoryObj<typeof meta>;

export const TreeFilterStory: Story = {
  args: {
    rootNodes: [
      {
        name: "Node 1",
        selected: "none",
        expanded: false,
        children: [],
        description: "Node 1 description",
      },
      {
        name: "Node 2",
        selected: "none",
        expanded: false,
        children: [],
      },
      {
        name: "Node 3",
        selected: "none",
        expanded: false,
        description: "Node 3 description",
        children: [
          {
            name: "Node 3.1",
            selected: "none",
            expanded: false,
            children: [],
          },
          {
            name: "Node 3.2",
            selected: "none",
            expanded: false,
            children: [],
          },
          {
            name: "Node 3.3",
            selected: "none",
            expanded: false,
            children: [],
          },
        ],
      },
    ],
  },
};

export const LargeTreeFilterStory: Story = {
  args: {
    rootNodes: generateTreeData(5, 3),
  },
};
