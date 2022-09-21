import Vue from "vue";
import VueRouter from "vue-router";
import ListTables from "./components/ListTables";
import ViewTable from "./components/ViewTable";

import "molgenis-components/dist/style.css";
// workaround for not importing recursive component
Vue.component("EditModal", EditModal);

import App from "./App";

Vue.use(VueRouter);

/** use vue router only to react to change url attributes */
const router = new VueRouter({
  routes: [
    {
      path: "/",
      component: ListTables,
      props: true,
    },
    {
      path: "/:table",
      component: ViewTable,
      props: true,
    },
  ],
});

new Vue({
  router,
  render: (h) => h(App),
}).$mount("#app");
