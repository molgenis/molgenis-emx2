import { createApp } from "vue";
import { createRouter, createWebHashHistory } from "vue-router";
import App from "./App.vue";
import Beacon from "./components/Beacon.vue";

import "molgenis-components/dist/style.css";

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: "/",
      component: Beacon,
      props: true,
    },
  ],
});

const app = createApp(App);
app.use(router);
app.mount("#app");
