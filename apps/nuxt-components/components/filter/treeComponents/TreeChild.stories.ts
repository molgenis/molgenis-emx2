import TreeChild from "./TreeChild.vue";
import type { Meta, StoryObj } from "@storybook/vue3";

const meta = {
  title: "filter/tree-components/TreeChild",
  component: TreeChild,
} satisfies Meta<typeof TreeChild>;

export default meta;
type Story = StoryObj<typeof meta>;

export const TreeChildStory: Story = {
  args: {
    nodes: [
      {
        name: "Node 1",
        selected: "complete",
        expanded: true,
        children: [
          {
            name: "Node 1.1",
            selected: "complete",
            expanded: true,
            children: [
              {
                name: "Node 1.1.1",
                selected: "complete",
                expanded: true,
              },
            ],
          },
        ],
        title: "TreeChild",
      },
    ],
  },
};
