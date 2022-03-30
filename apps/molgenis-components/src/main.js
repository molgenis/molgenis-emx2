import Vue from "vue";
import VueRouter from "vue-router";
import App from "./App.vue";
import ClientView from "./ClientView.vue";
import Sidebar from "./Sidebar.vue";
import DemoItem from "./DemoItem.vue";
import axios from "axios";
const components = import.meta.globEager("./components/**/*.vue");
const docs = import.meta.globEager("./docs/**/*.vue");

Object.entries(components).forEach(([path, definition]) => {
  // Get name of component, based on filename
  // "./components/Fruits.vue" will become "Fruits"
  const componentName = path
    .split("/")
    .pop()
    .replace(/\.\w+$/, "");

  // Register component on this Vue instance
  Vue.component(componentName, definition.default);
});

Vue.component("DemoItem", DemoItem);

const routes = [
  { path: "/", components: { sidebar: Sidebar } },
  { path: "/client", component: ClientView },
];

const docsMap = {}

// create routes for all the docs
Object.entries(docs).forEach(([path, definition]) => {
  const componentName = path
    .split("/")
    .pop()
    .replace(/\.\w+$/, "");

  routes[0].components[componentName] = definition.default; // for listing
  routes.push({ path: "/component/" + componentName, component: definition.default }) // for detail view
  const folderPath = path.split("/").slice(2) // remove folder root path
  folderPath.pop() // remove component name 
  docsMap[componentName] = { name: componentName, path: folderPath };
});

// global variable
Vue.prototype.$docsMap = docsMap;

Vue.use(VueRouter);
const router = new VueRouter({ routes });

// Add axios to demo app global vue as plugin, will not be part of exposed library
Vue.use({
  install(Vue) {
    Vue.prototype.$axios = axios;
  },
});

new Vue({
  router,
  render: (h) => h(App),
}).$mount("#app");
