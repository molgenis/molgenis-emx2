import { createApp } from "vue";
import { createRouter, createWebHashHistory } from "vue-router";
import App from "./App.vue";
import JobList from "./views/JobList.vue";
import JobDetail from "./views/JobDetail.vue";
import WorkerList from "./views/WorkerList.vue";
import ArtifactList from "./views/ArtifactList.vue";
import ArtifactDetail from "./views/ArtifactDetail.vue";

import "molgenis-components/dist/style.css";
import "./styles/hpc-theme.css";

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
    {
      path: "/artifacts",
      component: ArtifactList,
    },
    {
      path: "/artifacts/:id",
      component: ArtifactDetail,
      props: true,
    },
  ],
});

const app = createApp(App);
app.use(router);
app.mount("#app");
