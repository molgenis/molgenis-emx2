<template>
  <ButtonDropdown :label="title" v-if="visible">
    <slot />
  </ButtonDropdown>
</template>

<script>
import { ButtonDropdown } from "@molgenis/molgenis-components";

export default {
  components: {
    ButtonDropdown,
  },
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
