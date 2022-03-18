<template>
  <div
    class="bg-white p-2 border border-light rounded-sm"
    :class="{'m-n2': outsidePadding}"
  >
    <slot />
  </div>
</template>

<script>
export default {
  props: {
    outsidePadding: {
      type: Boolean,
      default: false
    }
  }
};
</script>

<docs>
Example
```
<Panel>This can wrap elements</Panel>
<br>
<Panel outsidePadding>This will put the padding outside of the panel</Panel>
```
</docs>
