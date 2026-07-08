<template>
  <div class="flex flex-col gap-8">
    <Demo id="large" title="Large">
      <div class="flex gap-4">
        <Button type="primary" size="large">Primary</Button>
        <Button type="secondary" size="large">Secondary</Button>
        <Button type="tertiary" size="large">tertiary</Button>
        <Button type="outline" size="large">outline</Button>
        <Button type="disabled" size="large">disabled</Button>
        <Button type="filterWell" size="large">filterWell</Button>
      </div>
    </Demo>

    <Demo id="medium" title="Medium">
      <div class="flex gap-4">
        <Button type="primary" size="medium">Primary</Button>
        <Button type="secondary" size="medium">Secondary</Button>
        <Button type="tertiary" size="medium">tertiary</Button>
        <Button type="outline" size="medium">outline</Button>
        <Button type="disabled" size="medium">disabled</Button>
        <Button type="filterWell" size="medium">filterWell</Button>
      </div>
    </Demo>

    <Demo id="small" title="Small">
      <div class="flex gap-4">
        <Button type="primary" size="small">Primary</Button>
        <Button type="secondary" size="small">Secondary</Button>
        <Button type="tertiary" size="small">tertiary</Button>
        <Button type="outline" size="small">outline</Button>
        <Button type="disabled" size="small">disabled</Button>
        <Button type="filterWell" size="small">filterWell</Button>
      </div>
    </Demo>

    <Demo id="tiny" title="Tiny">
      <div class="flex gap-4">
        <Button type="primary" size="tiny">Primary</Button>
        <Button type="secondary" size="tiny">Secondary</Button>
        <Button type="tertiary" size="tiny">tertiary</Button>
        <Button type="outline" size="tiny">outline</Button>
        <Button type="disabled" size="tiny">disabled</Button>
        <Button type="filterWell" size="tiny">filterWell</Button>
      </div>
    </Demo>

    <Demo id="icons" title="Icons">
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
    </Demo>

    <Demo id="icon-only" title="Icon only">
      <div class="flex flex-col gap-4">
        <h4 class="text-title">large</h4>
        <div class="flex gap-4">
          <Button iconOnly icon="plus" label="add" size="large" />
          <Button
            iconOnly
            icon="plus"
            label="add"
            type="secondary"
            size="large"
          />
          <Button
            iconOnly
            icon="plus"
            label="add"
            type="tertiary"
            size="large"
          />
          <Button
            iconOnly
            icon="plus"
            label="add"
            type="outline"
            size="large"
          />
          <Button
            iconOnly
            icon="plus"
            label="add"
            type="disabled"
            size="large"
          />
          <Button
            iconOnly
            icon="plus"
            label="add"
            type="filterWell"
            size="large"
          />
        </div>

        <h4 class="text-title">medium</h4>
        <div class="flex gap-4">
          <Button iconOnly icon="trash" label="Remove" size="medium" />
          <Button
            iconOnly
            icon="trash"
            label="Remove"
            type="secondary"
            size="medium"
          />
          <Button
            iconOnly
            icon="trash"
            label="Remove"
            type="tertiary"
            size="medium"
          />
          <Button
            iconOnly
            icon="trash"
            label="Remove"
            type="outline"
            size="medium"
          />
          <Button
            iconOnly
            icon="trash"
            label="Remove"
            type="disabled"
            size="medium"
          />
          <Button
            iconOnly
            icon="trash"
            label="Remove"
            type="filterWell"
            size="medium"
          />
        </div>

        <h4 class="text-title">small</h4>
        <div class="flex gap-4">
          <Button iconOnly icon="caret-up" label="previous" size="small" />
          <Button
            iconOnly
            icon="caret-up"
            label="previous"
            type="secondary"
            size="small"
          />
          <Button
            iconOnly
            icon="caret-up"
            label="previous"
            type="tertiary"
            size="small"
          />
          <Button
            iconOnly
            icon="caret-up"
            label="previous"
            type="outline"
            size="small"
          />
          <Button
            iconOnly
            icon="caret-up"
            label="previous"
            type="disabled"
            size="small"
          />
          <Button
            iconOnly
            icon="caret-up"
            label="previous"
            type="filterWell"
            size="small"
          />
        </div>

        <h4 class="text-title">tiny</h4>
        <div class="flex gap-4">
          <Button iconOnly icon="caret-down" label="next" size="tiny" />
          <Button
            iconOnly
            icon="caret-down"
            label="next"
            type="secondary"
            size="tiny"
          />
          <Button
            iconOnly
            icon="caret-down"
            label="next"
            type="tertiary"
            size="tiny"
          />
          <Button
            iconOnly
            icon="caret-down"
            label="next"
            type="outline"
            size="tiny"
          />
          <Button
            iconOnly
            icon="caret-down"
            label="next"
            type="disabled"
            size="tiny"
          />
          <Button
            iconOnly
            icon="caret-down"
            label="next"
            type="filterWell"
            size="tiny"
          />
        </div>
      </div>
    </Demo>

    <Demo id="text-button" title="Text button">
      <div class="flex flex-col gap-4">
        <div class="flex items-start gap-4">
          <Button type="text" size="small" iconPosition="left">
            Click me
          </Button>
          <Button type="text" size="small" icon="star" iconPosition="left">
            Star me
          </Button>
        </div>
      </div>
    </Demo>

    <Demo id="inline-button" title="Inline button">
      <div class="flex flex-col gap-4">
        <div class="flex gap-4">
          <Button type="inline" :icon-only="true" icon="plus" />
          <Button type="inline" :icon-only="true" icon="trash" />
          <Button type="inline" :icon-only="true" icon="edit" />
          <Button type="inline" :icon-only="true" icon="caret-up" />
          <Button type="inline" :icon-only="true" icon="caret-down" />
          <Button type="inline" icon="plus" label="Add" />
          <Button type="inline" icon="trash" label="Remove" />
          <Button type="inline" icon="edit" label="Edit" />
          <Button type="inline" icon="caret-up" label="Previous" />
          <Button type="inline" icon="caret-down" label="Next" />
        </div>
      </div>
    </Demo>

    <!-- do not render by default to avoid errors on clean load-->
    <div v-if="route.query.showError" class="flex gap-4">
      <!-- this should result in an error ( due to missing label)-->
      <Button iconOnly icon="plus"></Button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRoute } from "vue-router";

const route = useRoute();
</script>
