import  Comp1  from '../components/Comp1.vue'
import type { Meta, StoryObj } from '@storybook/vue3'


// More on how to set up stories at: https://storybook.js.org/docs/vue/writing-stories/introduction

const meta = {
  title: 'Comps/Comp1',
  component: Comp1,
  // This component will have an automatically generated docsPage entry: https://storybook.js.org/docs/vue/writing-docs/autodocs
  tags: ['autodocs'],

} satisfies Meta<typeof Comp1>

export default meta
type Story = StoryObj<typeof meta>

export const Comp1Story: Story = {
  args: {},
}
