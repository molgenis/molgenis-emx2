import { createApp } from "vue";
import { createRouter, createWebHashHistory } from "vue-router";
import App from "./App.vue";
import HelloWorld from "./components/HelloWorld.vue";
import SomeQuery from "./components/SomeQuery.vue";
import { createPinia } from "pinia";
import { useSessionStore } from "molgenis-components";

import "molgenis-components/dist/style.css";

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: "/",
      component: HelloWorld,
      props: true,
    },
    {
      path: "/somequery",
      component: SomeQuery,
      props: true,
    },
  ],
});

const app = createApp(App);

const pinia = createPinia();
app.use(pinia);
useSessionStore(pinia);

app.use(router);
app.mount("#app");
