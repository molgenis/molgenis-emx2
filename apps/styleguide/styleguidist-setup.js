import Vue from "vue";
Vue.component("RouterLink", {
  props: {
    tag: { type: String, default: "a" },
    to: { type: Object },
  },
  render(createElement) {
    return createElement(
      this.tag,
      { props: { to: this.to } },
      this.$slots.default
    );
  },
});
