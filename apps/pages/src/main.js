import { createApp } from "vue";
import { createRouter, createWebHashHistory } from "vue-router";
import App from "./App.vue";
import ListPages from "./components/ListPages.vue";
import ViewPage from "./components/ViewPage.vue";
import EditPage from "./components/EditPage.vue";

import "molgenis-components/dist/style.css";

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: "/",
      component: ListPages,
      props: true,
    },
    {
      path: "/:page",
      component: ViewPage,
      props: true,
    },
    {
      path: "/:page/edit",
      component: EditPage,
      props: true,
    },
  ],
});

const app = createApp(App);
app.use(router);
app.mount("#app");
