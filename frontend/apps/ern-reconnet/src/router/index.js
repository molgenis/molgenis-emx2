import { createRouter, createWebHashHistory } from "vue-router";

import HomePage from "../views/home-page.vue";
import AboutPage from "../views/about-page.vue";
import DocumentsPage from "../views/documents-page.vue";
import DashboardPage from "../views/dashboard-page.vue";

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      name: "home",
      path: "/",
      component: HomePage,
    },
    {
      name: "about-us",
      path: "/about-us",
      component: AboutPage,
      meta: {
        title: "About Us",
      },
    },
    {
      name: "documents",
      path: "/documents",
      component: DocumentsPage,
      meta: {
        title: "Documents",
      },
    },
    {
      name: "dashboard",
      path: "/dashboard",
      component: DashboardPage,
      meta: {
        title: "Dashboard",
      },
    },
  ],
  scrollBehavior() {
    return {
      top: 0,
    };
  },
});

router.afterEach((to) => {
  document.title = to.meta.title
    ? `${to.meta.title} | ERN ReCONNET`
    : "ERN ReCONNET";
});

export default router;
