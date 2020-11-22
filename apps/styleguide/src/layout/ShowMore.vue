<template>
  <div v-if="show">
    <slot />
    <a href="javascript:void(null)" @click="toggle">{{ hideLabel }}</a>
  </div>
  <a href="javascript:void(null)" v-else @click="toggle"
    >{{ showLabel }} <br
  /></a>
</template>

<script>
export default {
  props: { title: String },
  data() {
    return { show: false };
  },
  computed: {
    showLabel() {
      if (this.title != null) {
        return "show " + this.title;
      } else {
        return "...show more";
      }
    },
    hideLabel() {
      if (this.title != null) {
        return "hide " + this.title;
      } else {
        return "...show less";
      }
    },
  },
  methods: {
    toggle() {
      this.show = !this.show;
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
