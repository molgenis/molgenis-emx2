import Vue from "vue";
import App from "./App.vue";
import VueRouter from "vue-router";
import SchemaApp from "./components/SchemaBeta.vue";

Vue.config.productionTip = false;

Vue.use(VueRouter);

const router = new VueRouter({
  routes: [{ name: "simple", path: "/", component: SchemaApp }],
});

new Vue({
  router,
  render: (h) => h(App),
}).$mount("#app");
