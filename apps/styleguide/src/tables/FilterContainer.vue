<template>
  <div class="mb-2 pt-2 border-top bg-white" v-if="visible">
    <div
      class="filter-header d-flex flex-row justify-content-between"
      @click="
        expanded
          ? $emit('update:expanded', false)
          : $emit('update:expanded', true)
      "
    >
      <h6 class="mb-0 font-weight-bold p-2">
        {{ title }}
        <span v-if="count > 0" class="badge badge-secondary">{{ count }}</span>
      </h6>
      <IconAction
        class="ml-auto"
        :icon="expanded ? 'angle-up' : 'angle-down'"
      />
    </div>
    <slot name="header" />
    <div v-if="expanded" class="p-2">
      <!-- @slot Use this slot to place the filter box content -->
      <slot />
    </div>
  </div>
</template>

<style scoped>
.filter-header h6 {
  vertical-align: top;
}

.filter-header:hover {
  cursor: move;
}
</style>

<script>
import { IconAction } from "@molgenis/molgenis-components";

export default {
  components: {
    IconAction,
  },
  props: {
    title: String,
    visible: Boolean,
    conditions: Array,
    expanded: { type: Boolean, default: false },
  },
  data() {
    return { hover: false };
  },
  computed: {
    count() {
      return Array.isArray(this.conditions) ? this.conditions.length : null;
    },
  },
};
</script>

<docs>

Example expanded

```jsx
<template>
  <FilterContainer title="My filter" :visible="true" :expanded="true">
    some contents of the filter box
  </FilterContainer>
</template>
```

Example collapsed

```jsx
<template>
  <FilterContainer title="My filter" :visible="true" :expanded="false">
    some contents of the filter box
  </FilterContainer>
</template>
```

</docs>
