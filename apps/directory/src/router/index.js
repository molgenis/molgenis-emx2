import { createRouter, createWebHashHistory } from "vue-router";
import HomeView from "../views/HomeView.vue";
import AboutView from "../views/AboutView.vue";
import Landingpage from "../views/Landingpage.vue";
import BiobankReport from "../views/BiobankReport.vue";
import NetworkReport from "../views/NetworkReport.vue";
import ConfigurationScreen from "../views/ConfigurationScreen.vue";
import { useSettingsStore } from "../stores/settingsStore";

const router = createRouter({
  history: createWebHashHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: "/catalogue",
      name: "catalogue",
      component: HomeView,
    },
    {
      path: "/about",
      name: "about",
      component: AboutView,
    },
    {
      path: "/collection/:id",
      name: "collectiondetails",
      // route level code-splitting
      // this generates a separate chunk (About.[hash].js) for this route
      // which is lazy-loaded when the route is visited.
      component: () => import("../views/CollectionReport.vue"),
    },
    {
      path: "/biobank/:id",
      name: "biobankdetails",
      component: BiobankReport,
    },
    { path: "/network/:id", name: "networkdetails", component: NetworkReport },
    {
      path: "/configuration",
      component: ConfigurationScreen,
      beforeEnter: async (to, from, next) => {
        const settingsStore = useSettingsStore();
        await settingsStore.initializeConfig();
        if (settingsStore.showSettings) {
          next();
        } else next("/");
      },
    },
    {
      path: "/",
      component: Landingpage,
      beforeEnter: async (to, from, next) => {
        const settingsStore = useSettingsStore();
        await settingsStore.initializeConfig();
        if (
          settingsStore.config.landingpage.enabled &&
          !Object.keys(to.query).length
        ) {
          next();
        } else {
          next({ path: "/catalogue", query: to.query });
        }
      },
    },
  ],
});

export default router;
