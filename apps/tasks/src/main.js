import { createApp } from "vue/dist/vue.esm-bundler";
import { createRouter, createWebHashHistory } from "vue-router";
import App from "./App.vue";
import ListScripts from "./components/ListScripts.vue";
import ListJobs from "./components/ListJobs.vue";
import "molgenis-components/dist/style.css";

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: "/",
      redirect: { path: "/scripts" },
    },
    {
      path: "/scripts",
      component: ListScripts,
      props: true,
      name: "scripts",
    },
    {
      path: "/jobs",
      component: ListJobs,
      props: true,
      name: "jobs",
    },
  ],
});

const app = createApp(App);
app.use(router);
app.mount("#app");
