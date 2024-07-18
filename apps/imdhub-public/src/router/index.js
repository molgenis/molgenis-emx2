import { createRouter, createWebHashHistory } from "vue-router";

const project = "Recon4imd";

import ViewHome from "../views/view-home.vue";
import ViewAbout from "../views/view-about.vue";
import ViewDashboard from "../views/view-dashboard.vue";
import ViewDocuments from "../views/view-documents.vue";
import ViewRequestAccess from "../views/view-request-access.vue";

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      name: "home",
      path: "/",
      component: ViewHome,
    },
    {
      name: "about",
      path: "/about",
      component: ViewAbout,
      meta: {
        title: "About Us",
      },
    },
    {
      name: "dashboard",
      path: "/dashboard",
      component: ViewDashboard,
      meta: {
        title: "Dashboard",
      },
    },
    {
      name: "documents",
      path: "/documents",
      component: ViewDocuments,
      meta: {
        title: "Documents",
      },
    },
    {
      name: "request-access",
      path: "/request",
      component: ViewRequestAccess,
      meta: {
        title: "Request access",
      },
    },
  ],
  scrollBehavior(_to, _from, savedPosition) {
    return savedPosition || { top: 0, left: 0 };
  },
});

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} | ${project}` : project;
});

export default router;
