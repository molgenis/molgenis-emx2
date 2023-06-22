import { createApp } from "vue/dist/vue.esm-bundler";
import { createRouter, createWebHashHistory } from "vue-router";
import App from "./App.vue";
import ListTables from "./components/ListTables.vue";
import ViewTable from "./components/ViewTable.vue";
import { EditModal } from "molgenis-components";
import VueGtag from "vue-gtag";

import "molgenis-components/dist/style.css";

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: "/",
      component: ListTables,
      props: true,
    },
    {
      path: "/:table",
      component: ViewTable,
      props: true,
    },
  ],
});

const app = createApp(App);
app.use(router);
app.use(VueGtag, { bootstrap: false }, router);
// workaround for not importing recursive component
app.component("EditModal", EditModal);
app.mount("#app");
