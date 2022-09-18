import Vue from "vue";
import App from "./App.vue";
import "molgenis-components/dist/style.css";

Vue.config.productionTip = false;

new Vue({
  render: (h) => h(App),
}).$mount("#app");
