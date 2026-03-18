import { createApp } from "vue";
import { createRouter, createWebHashHistory } from "vue-router";
import App from "./App.vue";
import Import from "./components/Import.vue";
import Harvest from "./components/Harvest.vue";
import "../../molgenis-components/dist/molgenis-components.css";

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: "/",
      component: Import,
    },
    {
      path: "/harvest",
      component: Harvest,
    },
  ],
});

const app = createApp(App);
app.use(router);
app.mount("#app");
