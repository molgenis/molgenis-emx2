import StarSolid from "./StarSolid.vue";
import type { Meta, StoryObj } from "@storybook/vue3";

const meta = {
  title: "icons/StarSolid",
  component: StarSolid,
} satisfies Meta<typeof StarSolid>;

export default meta;
type Story = StoryObj<typeof meta>;

export const StarSolidStory: Story = {
  args: {},
};
