import { createApp } from "vue";
import { createRouter, createWebHashHistory } from "vue-router";
import App from "./App.vue";
import Schema from "./components/Schema.vue";
import PrintView from "./components/PrintView.vue";
import VueScrollTo from "vue-scrollto";

import "molgenis-components/dist/style.css";

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: "/", component: Schema },
    { path: "/print", component: PrintView },
  ],
});

const app = createApp(App);
app.use(router);
app.use(VueScrollTo);
app.mount("#app");
