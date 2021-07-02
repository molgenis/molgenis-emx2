import Vue from "vue";
/**
 * Mock for vue-router RouterLink component
 */
Vue.component("RouterLink", {
  props: {
    tag: { type: String, default: "a" },
    to: { type: Object, default: () => ({ path: "https://molgenis.org" }) },
  },
  render(createElement) {
    console.log(this.to);
    return createElement(
      this.tag,
      {
        attrs: {
          href: this.to.path,
        },
      },
      this.$slots.default
    );
  },
});
