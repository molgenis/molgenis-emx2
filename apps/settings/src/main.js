import Vue from "vue";
import VueRouter from "vue-router";
import App from "./App.vue";
import Members from "./components/Members";
import Layout from "./components/Layout";

Vue.config.productionTip = false;

Vue.use(VueRouter);

/** use vue router only to react to change url attributes */
const router = new VueRouter({
  routes: [
    {
      name: "Layout",
      path: "/Layout",
      component: Layout
    },
    {
      name: "Members",
      path: "/Members",
      component: Members
    },
    {
      path: "/",
      redirect: "/members"
    }
  ]
});

new Vue({
  router,
  render: h => h(App)
}).$mount("#app");
