// import Vue from "vue";
import { createApp } from "vue";
import { createRouter, createWebHashHistory } from "vue-router";
import App from "./App.vue";
import ClientView from "./ClientView.vue";
import Sidebar from "./Sidebar.vue";
import DemoItem from "./DemoItem.vue";
import axios from "axios";
import VueScrollTo from "vue-scrollto";
import Client from "./client/client.js";
import * as utils from "./components/utils";

const app = createApp(App);

const routes = [
  { path: "/", components: { sidebar: Sidebar } },
  { path: "/client", component: ClientView },
];

const components = import.meta.globEager("./components/**/*.vue");
const generatedDocumentComponents = import.meta.globEager(
  "../gen-docs/**/*.vue"
);

Object.entries(components).forEach(([path, definition]) => {
  // Get name of component, based on filename
  // "./components/Fruits.vue" will become "Fruits"
  const componentName = path
    .split("/")
    .pop()
    .replace(/\.\w+$/, "");

  // Register component on this Vue instance
  app.component(componentName, definition.default);
});

app.component("DemoItem", DemoItem);
app.component("Client", Client);

let docsMap = {};

// create routes for generated docs
Object.entries(generatedDocumentComponents).forEach(([path, definition]) => {
  const componentName = path
    .split("/")
    .pop()
    .replace(/\.\w+$/, "");

  routes[0].components[componentName] = definition.default; // for listing
  routes.push({
    path: "/component/" + componentName,
    component: definition.default,
  });
  // for detail view
  const folderPath = path.split("/").slice(3); // remove folder root path
  folderPath.pop(); // remove component name
  docsMap[componentName] = { name: componentName, path: folderPath };
});

// // global variable
// Vue.prototype.$docsMap = docsMap;
app.config.globalProperties.$docsMap = docsMap;
app.config.globalProperties.$Client = Client;
app.config.globalProperties.$utils = utils;

// Vue.use(VueRouter);
// const router = new VueRouter({ routes });

const router = createRouter({
  history: createWebHashHistory(),
  routes,
});

app.use(router);

app.mount("#app");

// // use for in page routing
app.use(VueScrollTo, {
  container: "#page-content-wrapper",
});

// // Add axios to demo app global vue as plugin, will not be part of exposed library
app.use({
  install(Vue) {
    app.config.globalProperties.$axios = axios;
  },
});

// new Vue({
//   router,
//   render: (h) => h(App),
// }).$mount("#app");
