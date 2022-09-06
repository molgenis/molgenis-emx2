import Vue from "vue";
import App from "./App.vue";
import VueRouter from "vue-router";
import Groups from "./components/Groups";
import Admin from "./components/admin/Admin";
import ManageSettings from "./components/admin/ManageSettings";
import ManageUsers from "./components/admin/ManageUsers";

import "molgenis-components/dist/style.css";

Vue.config.productionTip = false;

Vue.use(VueRouter);

const router = new VueRouter({
  routes: [
    { name: "central", path: "/", component: Groups },
    {
      name: "admin",
      path: "/admin",
      component: Admin,
      redirect: "/admin/users",
      children: [
        { name: "users", path: "users", component: ManageUsers },
        { name: "settings", path: "settings", component: ManageSettings },
      ],
    },
  ],
});

new Vue({
  router,
  render: (h) => h(App),
}).$mount("#app");
