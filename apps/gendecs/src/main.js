import Vue from "vue";
import VueRouter from "vue-router";

import App from "./App";
import GenDecs from "./components/GenDecs";

Vue.use(VueRouter);

/** use vue router only to react to change url attributes */
const router = new VueRouter({
  routes: [
    {
      path: "/",
      component: GenDecs,
      props: true,
    },
    {
      path: "/somequery",
      component: SomeQuery,
      props: true,
    },
  ],
});

new Vue({
  router,
  render: (h) => h(App),
}).$mount("#app");
