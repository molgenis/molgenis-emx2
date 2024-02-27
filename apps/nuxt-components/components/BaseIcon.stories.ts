import BaseIcon from "./BaseIcon.vue";
import type { Meta, StoryObj } from "@storybook/vue3";

const meta = {
  title: "icons/BaseIcon",
  component: BaseIcon,
} satisfies Meta<typeof BaseIcon>;

export default meta;
type Story = StoryObj<typeof meta>;

export const BaseIconStory: Story = {
  args: {},
};

export const Default = {
  args: {
    name: "caret-up",
  },
};
