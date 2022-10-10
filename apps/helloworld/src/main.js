import Vue from "vue";
import VueRouter from "vue-router";

import App from "./App.vue";
import HelloWorld from "./components/HelloWorld.vue";
import SomeQuery from "./components/SomeQuery.vue";

import "molgenis-components/dist/style.css";

Vue.use(VueRouter);

/** use vue router only to react to change url attributes */
const router = new VueRouter({
  routes: [
    {
      path: "/",
      component: HelloWorld,
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
