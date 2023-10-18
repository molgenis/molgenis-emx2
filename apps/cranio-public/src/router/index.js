import { createRouter, createWebHashHistory } from "vue-router";

import CranioHome from "../views/view-home.vue";
import AboutPage from "../views/view-about.vue";
import PublicDashboardPage from "../views/view-public-dashboard.vue";
import ProvidersPage from "../views/view-providers.vue";

const project = "ERN CRANIO";

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      name: "home",
      path: "/",
      component: CranioHome,
    },
    {
      name: "about",
      path: "/about",
      component: AboutPage,
      meta: {
        title: "About",
        breadcrumbs: [{ name: "about", label: "About" }],
      },
    },
    {
      name: "dashboard",
      path: "/dashboard",
      component: PublicDashboardPage,
      meta: {
        title: "Dashboard",
        breadcrumbs: [{ name: "dashboard", label: "Dashboard" }],
      },
    },
    {
      name: "providers",
      path: "/providers",
      component: ProvidersPage,
      meta: {
        title: "Providers",
        breadcrumbs: [{ name: "providers", label: "Providers" }],
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
