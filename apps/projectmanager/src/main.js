import { createApp } from "vue";
import { createRouter, createWebHashHistory } from "vue-router";
import App from "./App.vue";
import ProjectPlanning from "./components/ProjectPlanning.vue";
import PersonPlanning from "./components/PersonPlanning.vue";

import "molgenis-components/dist/style.css";

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: "/",
      component: ProjectPlanning,
      props: true,
    },
    {
      path: "/projects",
      component: ProjectPlanning,
      props: true,
    },
    {
      path: "/persons",
      component: PersonPlanning,
      props: true,
    },
  ],
});

const app = createApp(App);
app.use(router);
app.mount("#app");
