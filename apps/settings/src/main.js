import Vue from "vue";
import VueRouter from "vue-router";
import App from "./App.vue";
import Members from "./components/Members.vue";
import Theme from "./components/Theme.vue";
import MenuManager from "./components/MenuManager.vue";
import PageManager from "./components/PageManager.vue";
import ChangelogViewer from "./components/ChangelogViewer.vue";
import SettingsManager from "./components/SettingsManager.vue";

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
