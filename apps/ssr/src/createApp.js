import Vue from "vue";
import VueRouter from "vue-router";

import App from "./App";
import ErrorView from "./views/ErrorView";
import JsonView from "./views/JsonView";
import DetailsView from "./views/DetailsView";
import PetView from "./views/PetView";

export function createApp() {
  Vue.use(VueRouter);

  // create the router;
  const router = new VueRouter({
    routes: [
      {
        // Demonstration of custom views using nicer routes (must also be implemented server side in java!)
        path: "/Pet/:name/petview",
        component: PetView,
        props: { table: "Pet", filter: { name: "spike" } },
      },
      {
        //in case of client side, state via router
        path: "/:table/:filter/json",
        component: JsonView,
        props: true,
      },
      {
        //in case of client side, state via router
        path: "/:table/:filter/details",
        component: DetailsView,
        props: true,
      },
      {
        //default path
        path: "/:catchAll(.*)",
        component: ErrorView,
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
