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
<FilterDropdown title="test" :visible="true">some contents</FilterDropdown>
```
</docs>
