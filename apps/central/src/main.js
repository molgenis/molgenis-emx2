import { createApp } from "vue";
import { createRouter, createWebHashHistory } from "vue-router";
import App from "./App.vue";
import Groups from "./components/Groups.vue";
import Admin from "./components/admin/Admin.vue";
import ManageSettings from "./components/admin/ManageSettings.vue";
import ManageUsers from "./components/admin/ManageUsers.vue";
import ManagePrivacyPolicy from "./components/admin/ManagePrivacyPolicy.vue";
import "molgenis-components/dist/style.css";

const router = createRouter({
  history: createWebHashHistory(),
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

const app = createApp(App);
app.use(router);
app.mount("#app");
