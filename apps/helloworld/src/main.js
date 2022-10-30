import {createApp} from 'vue';
import { createRouter, createWebHistory } from 'vue-router';
import App from "./App.vue";
import HelloWorld from "./components/HelloWorld.vue";
import SomeQuery from "./components/SomeQuery.vue";

import "molgenis-components/dist/style.css";

const router = createRouter({  history: createWebHistory(),
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
app.use(router);
app.mount("#app")
