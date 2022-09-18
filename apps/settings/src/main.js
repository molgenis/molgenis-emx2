import Vue from "vue";
import VueRouter from "vue-router";
import App from "./App.vue";
import Members from "./components/Members";
import Theme from "./components/Theme";
import MenuManager from "./components/MenuManager";
import PageManager from "./components/PageManager";
import ChangelogViewer from "./components/ChangelogViewer";
import SettingsManager from "./components/SettingsManager"

import "molgenis-components/dist/style.css";

Vue.config.productionTip = false;

Vue.use(VueRouter);

/** use vue router only to react to change url attributes */
const router = new VueRouter({
  routes: [
    {
      name: "Theme",
      path: "/Theme",
      component: Theme,
    },
    {
      name: "Members",
      path: "/Members",
      component: Members,
    },
    {
      name: "Menu",
      path: "/Menu",
      component: MenuManager,
    },
    {
      name: "Pages",
      path: "/Pages",
      component: PageManager,
    },
    {
      name: "Changelog",
      path: "/changelog",
      component: ChangelogViewer,
    },
    {
      name: "Advanced settings",
      path: "/settings",
      component: SettingsManager,
    },
    {
      path: "/",
      redirect: "/members",
    },
  ],
});

new Vue({
  router,
  render: (h) => h(App),
}).$mount("#app");
