<template>
  <ButtonDropdown :label="title" v-if="visible">
    <slot />
  </ButtonDropdown>
</template>

<script>
export default {
  props: {
    title: String,
    visible: Boolean,
  },
};
</script>

<docs>
example
```
<FilterDropdown title="test">some contents</FilterDropdown>
```
</docs>
