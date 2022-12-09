import { createApp } from "vue";
import { createRouter, createWebHashHistory } from "vue-router";
import App from "./App.vue";
import SubmissionList from "./components/SubmissionList.vue";
import VueScrollTo from "vue-scrollto";

import "molgenis-components/dist/style.css";

const router = createRouter({
  history: createWebHashHistory(),
  routes: [{ path: "/", component: SubmissionList }],
});

const app = createApp(App);
app.use(router);
app.directive("scroll-to", VueScrollTo);
app.mount("#app");
