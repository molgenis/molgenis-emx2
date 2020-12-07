<template>
  <div class="filter-header border-bottom" v-if="visible">
    <div class="d-flex flex-row justify-content-between">
      <label class="mb-0 font-weight-bold p-2">
        {{ title }}
        <span v-if="count > 0" class="badge badge-secondary">{{ count }}</span>
      </label>
      <IconAction
        class="ml-auto"
        :icon="expanded ? 'angle-up' : 'angle-down'"
        @click="expanded ? $emit('collapse') : $emit('expand')"
      />
    </div>
    <slot name="header" />
    <div v-if="expanded">
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
import IconAction from "../forms/IconAction";

export default {
  components: {
    IconAction,
  },
  props: {
    title: String,
    visible: Boolean,
    count: Number,
    expanded: { type: Boolean, default: false },
  },
  data() {
    return { hover: false };
  },
};
</script>

<docs>

Example

```jsx
<template>
  <FilterContainer title="My filter">
    some contents
  </FilterContainer>
</template>
```

</docs>
