import Vue from "vue";
import VueRouter from "vue-router";

import App from "./App";
import ListPages from "./components/ListPages";
import ViewPage from "./components/ViewPage";
import EditPage from "./components/EditPage";

import "molgenis-components/dist/style.css";

Vue.use(VueRouter);

/** use vue router only to react to change url attributes */
const router = new VueRouter({
  routes: [
    {
      path: "/",
      component: ListPages,
      props: true
    },
    {
      path: "/:page",
      component: ViewPage,
      props: true
    },
    {
      path: "/:page/edit",
      component: EditPage,
      props: true
    }
  ]
});

new Vue({
  router,
  render: h => h(App)
}).$mount("#app");
