import { createApp } from "vue";
import { createRouter, createWebHashHistory } from "vue-router";
import App from "./App.vue";
import Beacon from "./components/Beacon.vue";
import Welcome from "./components/Welcome.vue";

import "molgenis-components/dist/style.css";

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: "/beacon",
      component: Beacon,
      props: true,
    },
    {
      path: "/",
      component: Welcome,
      props: true,
    },
  ],
});

const app = createApp(App);
app.use(router);
app.mount("#app");
