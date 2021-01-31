import Vue from "vue";
import App from "./App";
import VueRouter from "vue-router";
import FormEditor from "./components/FormEditor";
import Schema from "./components/Schema";

Vue.config.productionTip = false;

Vue.use(VueRouter);

const router = new VueRouter({
  routes: [
    { name: "schema", path: "/", component: Schema },
    { name: "formeditor", path: "/formeditor", component: FormEditor },
  ],
});

new Vue({
  router,
  render: (h) => h(App),
}).$mount("#app");
