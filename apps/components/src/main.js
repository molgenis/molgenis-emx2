import Vue from "vue";
import App from "./App.vue";

Vue.config.ignoredElements = [/doc/]

new Vue({
  render: (h) => h(App),
}).$mount("#app");
