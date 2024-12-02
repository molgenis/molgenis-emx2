import { createApp } from "vue/dist/vue.esm-bundler";
import { createRouter, createWebHashHistory } from "vue-router";
import App from "./App.vue";
import ListTemplates from "./components/ListTemplates.vue";
import "molgenis-components/dist/style.css";

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: "/",
      redirect: { path: "/templates" },
    },
    {
      path: "/templates",
      component: ListTemplates,
      props: true,
      name: "scripts",
    },
  ],
});

const app = createApp(App);
app.use(router);
app.mount("#app");
