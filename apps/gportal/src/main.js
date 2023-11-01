import { createApp } from "vue";
import { createRouter, createWebHashHistory } from "vue-router";

import App from "./App.vue";
import HomePage from "./views/view-home.vue";
import DatasetSearch from "./views/search-dataset.vue";
import BeaconSearch from "./views/search-beacon.vue";

import "molgenis-components/dist/style.css";
import "molgenis-viz/dist/style.css";

const project = "Local GDI Portal";

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      name: "home",
      path: "/",
      component: HomePage,
      props: true,
    },
    {
      name: "datasets",
      path: "/datasets",
      component: DatasetSearch,
      props: true,
    },
    {
      name: "beacon",
      path: "/beacon",
      component: BeaconSearch,
      props: true,
    },
  ],
  scrollBehavior(to, from, savedPosition) {
    return savedPosition || { top: 0, left: 0 };
  },
});

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} | ${project}` : project;
});

const app = createApp(App);
app.use(router);
app.mount("#app");
