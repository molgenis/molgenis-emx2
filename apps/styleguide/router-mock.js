import Vue from "vue";

Vue.component("RouterLink", {
  props: {
    tag: { type: String, default: "a" },
  },
  render(createElement) {
    const href = this.$attrs.to;
    return createElement(
      this.tag,
      {
        attrs: { href },
      },
      this.$slots.default
    );
  },
});
