import { createApp } from "vue";
import { createRouter, createWebHashHistory } from "vue-router";
import App from "./App.vue";
import JobList from "./views/JobList.vue";
import JobDetail from "./views/JobDetail.vue";
import WorkerList from "./views/WorkerList.vue";

import "molgenis-components/dist/style.css";

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: "/",
      component: JobList,
    },
    {
      path: "/jobs/:id",
      component: JobDetail,
      props: true,
    },
    {
      path: "/workers",
      component: WorkerList,
    },
  ],
});

const app = createApp(App);
app.use(router);
app.mount("#app");
