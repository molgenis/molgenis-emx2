// we use this bundle to enable template at runtime
import { createApp } from "vue/dist/vue.esm-bundler";
import { createRouter, createWebHashHistory } from "vue-router";
import App from "./App.vue";
import ClientView from "./ClientView.vue";
import Sidebar from "./Sidebar.vue";
import DemoItem from "./DemoItem.vue";
import axios from "axios";
import VueScrollTo from "vue-scrollto";
import Client from "./client/client";
import * as utils from "./components/utils";
import constants from "./components/constants";

//load the components
const components = import.meta.globEager("./components/**/*.vue");
const generatedDocumentComponents = import.meta.globEager(
  "../gen-docs/**/*.vue"
);

let docsMap = {};

const routes = [
  { path: "/", components: { sidebar: Sidebar } },
  { path: "/client", component: ClientView },
];

// define routes for generated docs; add docsMap
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

// construct app
const app = createApp(App);

//add tools
app.config.globalProperties.$axios = axios;
app.config.globalProperties.$Client = Client;
app.config.globalProperties.$utils = utils;
app.config.globalProperties.$docsMap = docsMap;
app.config.globalProperties.window = window;
app.config.globalProperties.$constants = constants;

//add directives
app.directive("scroll-to", VueScrollTo);

//add components
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

// connect the router
const router = createRouter({ history: createWebHashHistory(), routes });
app.use(router);

// render the whole thing
app.mount("#app");
