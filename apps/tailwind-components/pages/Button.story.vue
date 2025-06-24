<template>
  <div class="flex flex-col gap-4">
    <h3 class="text-2xl text-title">Large</h3>
    <div class="flex gap-4">
      <Button type="primary" size="large">Primary</Button>
      <Button type="secondary" size="large">Secondary</Button>
      <Button type="tertiary" size="large">tertiary</Button>
      <Button type="outline" size="large">outline</Button>
      <Button type="disabled" size="large">disabled</Button>
      <Button type="filterWell" size="large">filterWell</Button>
    </div>

    <h3 class="text-2xl text-title">Medium</h3>
    <div class="flex gap-4">
      <Button type="primary" size="medium">Primary</Button>
      <Button type="secondary" size="medium">Secondary</Button>
      <Button type="tertiary" size="medium">tertiary</Button>
      <Button type="outline" size="medium">outline</Button>
      <Button type="disabled" size="medium">disabled</Button>
      <Button type="filterWell" size="medium">filterWell</Button>
    </div>

    <h3 class="text-2xl text-title">Small</h3>
    <div class="flex gap-4">
      <Button type="primary" size="small">Primary</Button>
      <Button type="secondary" size="small">Secondary</Button>
      <Button type="tertiary" size="small">tertiary</Button>
      <Button type="outline" size="small">outline</Button>
      <Button type="disabled" size="small">disabled</Button>
      <Button type="filterWell" size="small">filterWell</Button>
    </div>

    <h3 class="text-2xl text-title">Tiny</h3>
    <div class="flex gap-4">
      <Button type="primary" size="tiny">Primary</Button>
      <Button type="secondary" size="tiny">Secondary</Button>
      <Button type="tertiary" size="tiny">tertiary</Button>
      <Button type="outline" size="tiny">outline</Button>
      <Button type="disabled" size="tiny">disabled</Button>
      <Button type="filterWell" size="tiny">filterWell</Button>
    </div>

    <h3 class="text-2xl text-title">Icons</h3>
    <div class="flex gap-4">
      <Button type="primary" size="medium" icon="filter" iconPosition="left">
        Filter
      </Button>
      <Button type="primary" size="small" icon="filter" iconPosition="right">
        Filter
      </Button>
      <Button type="secondary" size="small" icon="star" iconPosition="left">
        Star
      </Button>
      <Button
        type="secondary"
        size="small"
        icon="CaretDown"
        iconPosition="right"
      >
        CaretDown
      </Button>
    </div>
    <h3 class="text-2xl text-title">Icon only</h3>
    <div class="flex flex-col gap-4">
      <div class="flex gap-4">
        <Button iconOnly icon="plus" label="add" />
        <Button iconOnly icon="plus" label="add" type="secondary" />
        <Button iconOnly icon="plus" label="add" type="tertiary" />
        <Button iconOnly icon="plus" label="add" type="outline" />
        <Button iconOnly icon="plus" label="add" type="disabled" />
        <Button iconOnly icon="plus" label="add" type="filterWell" />
      </div>

      <div class="flex gap-4">
        <Button iconOnly icon="trash" label="Remove" />
        <Button iconOnly icon="trash" label="Remove" type="secondary" />
        <Button iconOnly icon="trash" label="Remove" type="tertiary" />
        <Button iconOnly icon="trash" label="Remove" type="outline" />
        <Button iconOnly icon="trash" label="Remove" type="disabled" />
        <Button iconOnly icon="trash" label="Remove" type="filterWell" />
      </div>

      <div class="flex gap-4">
        <Button iconOnly icon="caret-up" label="previous" />
        <Button iconOnly icon="caret-up" label="previous" type="secondary" />
        <Button iconOnly icon="caret-up" label="previous" type="tertiary" />
        <Button iconOnly icon="caret-up" label="previous" type="outline" />
        <Button iconOnly icon="caret-up" label="previous" type="disabled" />
        <Button iconOnly icon="caret-up" label="previous" type="filterWell" />
      </div>

      <div class="flex gap-4">
        <Button iconOnly icon="caret-down" label="next" />
        <Button iconOnly icon="caret-down" label="next" type="secondary" />
        <Button iconOnly icon="caret-down" label="next" type="tertiary" />
        <Button iconOnly icon="caret-down" label="next" type="outline" />
        <Button iconOnly icon="caret-down" label="next" type="disabled" />
        <Button iconOnly icon="caret-down" label="next" type="filterWell" />
      </div>
      <div class="flex flex-col items-start gap-4">
        <Button type="text" size="small" iconPosition="left">
          Text button
        </Button>
        <Button type="text" size="small" icon="star" iconPosition="left">
          Text button with icon
        </Button>
      </div>
      <!-- do not render by default to avoid errors on clean load-->
      <div v-if="route.query.showError" class="flex gap-4">
        <!-- this should result in an error ( due to missing label)-->
        <Button iconOnly icon="plus"></Button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRoute } from "vue-router";

const route = useRoute();
</script>
