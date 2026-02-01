<script setup lang="ts">
import { ref } from "vue";
import Toggle from "../components/Toggle.vue";
import Story from "../components/Story.vue";

const spec = `
## Features
- Pill-shaped toggle for switching between options
- Supports 2 or more options
- Each option has icon and label
- Active state highlighted with theme color
- Uses v-model for two-way binding
- Theme-aware colors (works in all themes)

## Props
| Prop | Type | Default | Description |
|------|------|---------|-------------|
| options | { name: string; label: string; icon: string }[] | required | Available options |
| modelValue | string | required | Currently selected option name |
| size | 'tiny' \\| 'small' \\| 'medium' \\| 'large' | medium | Button size (matches Button sizes) |

## Auto Color Detection
Colors automatically adapt based on parent background:
- Inside \`.bg-form\`: uses input colors (for form contexts)
- Elsewhere: uses page colors (search-results-view-tabs)

## Emits
| Event | Payload | Description |
|-------|---------|-------------|
| update:modelValue | string | Emitted when option is selected |

## Usage Examples
- View toggle: table vs cards layout
- Mode toggle: edit vs preview
- Display toggle: list vs grid

## Test Checklist
- [ ] Renders all options
- [ ] Shows active state on selected option
- [ ] Click option updates modelValue
- [ ] Icons display correctly
- [ ] Pill shape with rounded ends
- [ ] Works with 2 options
- [ ] Works with 3+ options
- [ ] Works in Light theme
- [ ] Works in Dark theme
- [ ] Works in Molgenis theme
- [ ] Works in UMCG theme
- [ ] Works in AUMC theme
`;

const viewMode = ref("table");
const displayMode = ref("list");
const threeWay = ref("one");
</script>

<template>
  <Story title="Toggle" :spec="spec">
    <div class="space-y-8">
      <div class="p-6">
        <h3 class="text-lg font-bold mb-4 text-title">Sizes</h3>
        <div class="flex flex-col gap-4 items-start">
          <div class="flex items-center gap-4">
            <span class="text-sm w-16">tiny:</span>
            <Toggle
              v-model="viewMode"
              size="tiny"
              :options="[
                { name: 'table', label: 'Table', icon: 'view-table' },
                { name: 'cards', label: 'Cards', icon: 'view-compact' }
              ]"
            />
          </div>
          <div class="flex items-center gap-4">
            <span class="text-sm w-16">small:</span>
            <Toggle
              v-model="viewMode"
              size="small"
              :options="[
                { name: 'table', label: 'Table', icon: 'view-table' },
                { name: 'cards', label: 'Cards', icon: 'view-compact' }
              ]"
            />
          </div>
          <div class="flex items-center gap-4">
            <span class="text-sm w-16">medium:</span>
            <Toggle
              v-model="viewMode"
              size="medium"
              :options="[
                { name: 'table', label: 'Table', icon: 'view-table' },
                { name: 'cards', label: 'Cards', icon: 'view-compact' }
              ]"
            />
          </div>
          <div class="flex items-center gap-4">
            <span class="text-sm w-16">large:</span>
            <Toggle
              v-model="viewMode"
              size="large"
              :options="[
                { name: 'table', label: 'Table', icon: 'view-table' },
                { name: 'cards', label: 'Cards', icon: 'view-compact' }
              ]"
            />
          </div>
        </div>
        <p class="text-sm text-body-base mt-2">Selected: {{ viewMode }}</p>

        <h3 class="text-lg font-bold mb-4 mt-6 text-title">Three Options</h3>
        <Toggle
          v-model="threeWay"
          :options="[
            { name: 'one', label: 'One', icon: 'check' },
            { name: 'two', label: 'Two', icon: 'check' },
            { name: 'three', label: 'Three', icon: 'check' }
          ]"
        />
        <p class="text-sm text-body-base mt-2">Selected: {{ threeWay }}</p>
      </div>

      <div class="bg-form p-6 rounded-lg">
        <h3 class="text-lg font-bold mb-4 text-title">On Form Background</h3>
        <Toggle
          v-model="displayMode"
          :options="[
            { name: 'list', label: 'List', icon: 'list' },
            { name: 'grid', label: 'Grid', icon: 'view-compact' }
          ]"
        />
        <p class="text-sm text-body-base mt-2">Selected: {{ displayMode }}</p>

        <h3 class="text-lg font-bold mb-4 mt-6 text-title">Three Options</h3>
        <Toggle
          v-model="threeWay"
          :options="[
            { name: 'one', label: 'One', icon: 'check' },
            { name: 'two', label: 'Two', icon: 'check' },
            { name: 'three', label: 'Three', icon: 'check' }
          ]"
        />
        <p class="text-sm text-body-base mt-2">Selected: {{ threeWay }}</p>
      </div>
    </div>
  </Story>
</template>
