<template>
  <div v-if="show">
    <ButtonAlt @click="show = false">{{ hideLabel }}</ButtonAlt>
    <slot />
  </div>
  <ButtonAlt v-else @click="show = true">{{ showLabel }} <br /></ButtonAlt>
</template>

<script>
import ButtonAlt from "../forms/ButtonAlt";

export default {
  components: { ButtonAlt },
  props: {
    title: { type: String, default: "debug" },
  },
  data() {
    return { show: false };
  },
  computed: {
    showLabel() {
      return "show " + this.title;
    },
    hideLabel() {
      return "hide " + this.title;
    },
  },
};
</script>

<docs>
Without title
```
<ShowMore>
  This is a test
</ShowMore>
```
With title
```
<ShowMore title="debug">
  <pre>This is a test</pre>
</ShowMore>
```
</docs>
