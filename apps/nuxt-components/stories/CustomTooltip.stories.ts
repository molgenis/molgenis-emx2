import CustomTooltip from "./CustomTooltip.vue";
import type { Meta, StoryObj } from "@storybook/vue3";

const meta = {
  title: "icons/CustomTooltip",
  component: CustomTooltip,
};

export default meta;
type Story = StoryObj<typeof meta>;

export const CustomTooltipStory: Story = {
  args: {},
};

export const Default = {
  args: {
    label: "my-label",
    content: "my-content",
  },
};

export const Blue = {
  args: {
    label: "my-label",
    content: "my-content",
    hoverColor: "blue",
  },
};

export const White = {
  args: {
    label: "my-label",
    content: "my-content",
    hoverColor: "white",
  },
};

export const None = {
  args: {
    label: "my-label",
    content: "my-content",
    hoverColor: "none",
  },
};
