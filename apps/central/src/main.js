import Vue from "vue";
import App from "./App.vue";
import VueRouter from "vue-router";
import Groups from "./components/Groups";
import Admin from "./components/Admin";

Vue.config.productionTip = false;

Vue.use(VueRouter);

const router = new VueRouter({
  routes: [
    { name: "central", path: "/", component: Groups },
    { name: "admin", path: "/admin", component: Admin },
  ],
});

new Vue({
  router,
  render: (h) => h(App),
}).$mount("#app");
