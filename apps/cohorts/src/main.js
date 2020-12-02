import Vue from "vue";
import VueRouter from "vue-router";
import App from "./App.vue";
import VariablesView from "./views/VariablesView";
import CollectionsView from "./views/CollectionsView";

Vue.config.productionTip = false;

Vue.use(VueRouter);

const router = new VueRouter({
  routes: [
    {
      name: "Variables",
      path: "/variables",
      component: VariablesView,
    },
    {
      name: "Collections",
      path: "/collections",
      component: CollectionsView,
    },
    {
      path: "/",
      redirect: "/collections",
    },
  ],
});

new Vue({
  router,
  render: (h) => h(App),
}).$mount("#app");
