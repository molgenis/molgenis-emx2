import Vue from "vue";
import VueRouter from "vue-router";
import App from "./App.vue";
import ErrorView from "./views/ErrorView";

import JsonView from "./views/JsonView";

// import DetailsView from "./views/DetailsView";

export function createApp() {
  Vue.use(VueRouter);

  // create the router;
  const router = new VueRouter({
    routes: [
      {
        path: "/",
        component: ErrorView,
      },
      {
        //in case of server side, state is static
        path: "/json",
        component: JsonView,
      },
      {
        //in case of client side, state via router
        path: "/:table/:filter/json",
        component: JsonView,
      },
      //big todo: cannot yet include, somewhere 'document' is called
      // {
      //   //in case of server side, state is static
      //   path: "/details",
      //   component: DetailsView,
      // },
      // {
      //   //in case of client side, state via router
      //   path: "/:table/:filter/details",
      //   component: DetailsView,
      // },
    ],
  });

  //create the app
  const app = new Vue({
    router,
    render: (h) => h(App),
  });

  return { app, router };
}
