import { createApp } from "vue/dist/vue.esm-bundler";
import { createRouter, createWebHashHistory } from "vue-router";
import App from "./App.vue";
import ViewReport from "./components/ViewEditReport.vue";
import ListReports from "./components/ListReports.vue";

import "molgenis-components/dist/style.css";

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: "/",
      component: ListReports,
      props: true,
    },
    {
      path: "/:id",
      name: "edit",
      component: ViewReport,
      props: true,
    },
  ],
});

const app = createApp(App);
app.use(router);
app.mount("#app");
