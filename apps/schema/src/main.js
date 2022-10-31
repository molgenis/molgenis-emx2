import {createApp} from 'vue';
import { createRouter, createWebHistory } from 'vue-router';
import App from "./App.vue";
import Schema from "./components/Schema.vue";
import VueScrollTo from "vue-scrollto";


import "molgenis-components/dist/style.css";

const router = createRouter({  history: createWebHistory(),
  routes: [{ name: "simple", path: "/", component: Schema }],
});

const app = createApp(App);
app.use(router);
app.directive('scroll-to', VueScrollTo);
app.mount("#app");