import TreeFilter from "./TreeFilter.vue";
import type { Meta, StoryObj } from "@storybook/vue3";

const meta = {
  title: "fiter/TreeFilter",
  component: TreeFilter,
} satisfies Meta<typeof TreeFilter>;

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
