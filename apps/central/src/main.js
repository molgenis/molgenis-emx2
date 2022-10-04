import Vue from "vue";
import App from "./App.vue";
import VueRouter from "vue-router";
import Groups from "./components/Groups.vue";
import Admin from "./components/admin/Admin.vue";
import ManageSettings from "./components/admin/ManageSettings.vue";
import ManageUsers from "./components/admin/ManageUsers.vue";
import ManagePrivacyPolicy from "./components/admin/ManagePrivacyPolicy.vue";
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
        {
          name: "privacyPolicy",
          path: "privacyPolicy",
          component: ManagePrivacyPolicy,
        },
      ],
    },
  ],
});

new Vue({
  router,
  render: (h) => h(App),
}).$mount("#app");
