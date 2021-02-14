import Vue from "vue";
import App from "./App";
import VueRouter from "vue-router";
import SchemaSimple from "./components/SchemaSimple";

Vue.config.productionTip = false;

Vue.use(VueRouter);

const router = new VueRouter({
  routes: [{ name: "simple", path: "/", component: SchemaSimple }],
});

new Vue({
  router,
  render: (h) => h(App),
}).$mount("#app");
