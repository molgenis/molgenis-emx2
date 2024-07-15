import { createRouter, createWebHashHistory } from "vue-router";

import HomePage from "../views/view-home.vue";
import AboutPage from "../views/view-about.vue";
import DocumentsPages from "../views/view-documents.vue";
import PublicDashboard from "../views/view-public-dashboard.vue";

const project = "ERN SKIN";

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      name: "home",
      path: "/",
      component: HomePage,
    },
    {
      name: "about",
      path: "/about",
      component: AboutPage,
      meta: {
        title: "About Us",
      },
    },
    {
      name: "dashboard",
      path: "/dashboard",
      component: PublicDashboard,
      meta: {
        title: "Dashboard",
      },
    },
    {
      name: "documents",
      path: "/documents",
      component: DocumentsPages,
      meta: {
        title: "Documents",
      },
    },
  ],
  scrollBehavior(to, from, savedPosition) {
    return savedPosition || { top: 0, left: 0 };
  },
});

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} | ${project}` : project;
});

export default router;
