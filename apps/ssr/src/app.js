import Vue from "vue";
import VueRouter from "vue-router";
import App from "./App.vue";
import Test from "./views/Test";

Vue.use(VueRouter);

export function createApp() {
  // create the router;
  const router = new VueRouter({
    routes: [
      {
        path: "/",
        component: Test,
        props: true,
      },
    ],
  });

  //create the app
  const app = new Vue({
    router,
    render: (h) => h(App),
  });

  return { app, router };
}
