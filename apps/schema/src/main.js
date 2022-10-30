import {createApp} from 'vue';
import { createRouter, createWebHistory } from 'vue-router';
import App from "./App.vue";
import SchemaApp from "./components/SchemaApp.vue";

import "molgenis-components/dist/style.css";

const router = createRouter({  history: createWebHistory(),
  routes: [{ name: "simple", path: "/", component: SchemaApp }],
});

const app = createApp(App);
app.use(router);
app.mount("#app")
