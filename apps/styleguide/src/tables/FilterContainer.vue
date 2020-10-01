<template>
  <div class="card mb-3" v-if="visible">
    <div ref="header" class="card-header text-center">
      <h6>{{ title }}</h6>
      <IconAction icon="times" class="filter-remove" @click="$emit('remove')" />
      <slot name="header" />
    </div>
    <div class="card-body">
      <!-- @slot Use this slot to place the filter box content -->
      <slot />
    </div>
  </div>
</template>

<style>
.card-header:hover {
  cursor: move;
}

.card-header:hover .filter-remove {
  visibility: visible;
}

.filter-remove {
  float: right;
  position: absolute;
  top: 0px;
  right: 0px;
  visibility: hidden;
}

.card-header h6 {
  margin: 0px;
}
</style>

<script>
import IconAction from "../forms/IconAction";

export default {
  components: {
    IconAction
  },
  props: {
    title: String,
    visible: Boolean
  }
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
