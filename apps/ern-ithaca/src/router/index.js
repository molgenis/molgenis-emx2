import { createRouter, createWebHashHistory } from "vue-router";

import HomeView from "../views/view-home.vue";
import AboutView from "../views/view-about.vue";
import DashboardView from "../views/view-dashboard.vue";
import DocumentsView from "../views/view-documents.vue";
import TransparencyView from "../views/view-transparency.vue";

const project = "ERN ITHACA";

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      name: "home",
      path: "/",
      component: HomeView,
    },
    {
      name: "about",
      path: "/about",
      component: AboutView,
      meta: {
        title: "About",
      },
    },
    {
      name: "dashboard",
      path: "/dashboard",
      component: DashboardView,
      meta: {
        title: "Dashboard",
      },
    },
    {
      name: "documents",
      path: "/documents",
      component: DocumentsView,
      meta: {
        title: "Documents",
      },
    },
    {
      name: "transparency",
      path: "/transparency",
      component: TransparencyView,
      meta: {
        title: "Transparency Policy",
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
