import { createApp } from "vue";
import { createRouter, createWebHashHistory } from "vue-router";

import App from "./App.vue";
import Beacon from "./components/Beacon.vue";
// import Datasets from "./components/Datasets.vue";

import HomePage from "./views/view-home.vue";
import DatasetSearch from "./views/search-dataset.vue";

import "molgenis-components/dist/style.css";
import "molgenis-viz/dist/style.css";

const project = "Local GDI Portal";

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      name: "beacon",
      path: "/beacon",
      component: Beacon,
      props: true,
    },
    {
      name: "datasets",
      path: "/datasets",
      component: DatasetSearch,
      props: true,
    },
    {
      name: "home",
      path: "/",
      component: HomePage,
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
