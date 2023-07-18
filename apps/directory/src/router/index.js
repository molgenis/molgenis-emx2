import { createRouter, createWebHashHistory } from "vue-router";
import HomeView from "../views/HomeView.vue";
import AboutView from "../views/AboutView.vue";
import BiobankReport from "../views/BiobankReport.vue";
import NetworkReport from "../views/NetworkReport.vue";

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
      component: () => import("../views/CollectionReport.vue"),
    },
    {
      path: "/biobank/:id",
      name: "biobankdetails",
      component: BiobankReport,
    },
    { path: "/network/:id", name: "networkdetails", component: NetworkReport },
    {
      path: "/configuration",
      component: () =>
        // import(
        //   /* webpackChunkName: "configuration-screen" */ "../views/ConfigTest.vue"
        // ),
        import(
          /* webpackChunkName: "configuration-screen" */ "../views/ConfigurationScreen.vue"
        ),
      // todo check
      // beforeEnter: async (to, from, next) => {
      //   const response = await api.get('/app-ui-context')
      //   if (response.roles.includes('ROLE_SU')) { next() } else next('/')
      // }
    },
    {
      path: "/",
      name: "home",
      component: HomeView,
    },
  ],
});

export default router;
