import Vue from "vue";
import App from "./App.vue";
const components = import.meta.globEager("./components/**/*.vue");

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

new Vue({
  render: (h) => h(App),
}).$mount("#app");
