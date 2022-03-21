<template>
  <div
    :class="{
      'ml-n2 mr-n2 mb-2': outsidePadding && !hideStyle,
      'bg-white p-2 border border-light rounded-sm': !hideStyle
    }"
  >
    <slot />
  </div>
</template>

<script>
export default {
  props: {
    hideStyle: {
      type: Boolean,
      default: false
    },
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
<Panel hideStyle>This hides the styling of the panel</Panel>
```
</docs>
