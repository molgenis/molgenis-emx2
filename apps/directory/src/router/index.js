import { createRouter, createWebHashHistory } from "vue-router";
import HomeView from "../views/HomeView.vue";
import AboutView from "../views/AboutView.vue";
import BiobankReport from "../views/BiobankReport.vue";

const router = createRouter({
  history: createWebHashHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: "/catalogue",
      name: "catalogue",
      component: HomeView,
    },
    {
      path: "/about",
      name: "about",
      component: AboutView,
    },
    {
      path: "/collection/:id",
      name: "collectiondetails",
      // route level code-splitting
      // this generates a separate chunk (About.[hash].js) for this route
      // which is lazy-loaded when the route is visited.
      component: () => import("../views/AboutView.vue"),
    },
    {
      path: "/biobank/:id",
      name: "biobankdetails",
      component: BiobankReport,
    },
    {
      path: "/",
      name: "home",
      component: HomeView,
    },
  ],
});

export default router;
