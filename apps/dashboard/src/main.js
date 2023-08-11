import { createApp } from "vue";
import { createRouter, createWebHashHistory } from "vue-router";
import App from "./App.vue";
import Page from "./components/Page.vue";

import "molgenis-components/dist/style.css";

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: "/",
      component: Page,
      props: true,
    },
    {
      path: "/:page",
      component: Page,
      props: true,
    },
  ],
});

const app = createApp(App);
app.use(router);
app.mount("#app");
