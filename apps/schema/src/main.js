import { createApp } from "vue";
import { createRouter, createWebHashHistory } from "vue-router";
import App from "./App.vue";
import Schema from "./components/Schema.vue";
import VueScrollTo from "vue-scrollto";
import { createPinia } from "pinia";
import { useSessionStore } from "molgenis-components";

import "molgenis-components/dist/style.css";

const router = createRouter({
  history: createWebHashHistory(),
  routes: [{ path: "/", component: Schema }],
});

const app = createApp(App);

const pinia = createPinia();
app.use(pinia);
useSessionStore(pinia);

app.use(router);
app.use(VueScrollTo);
app.mount("#app");
