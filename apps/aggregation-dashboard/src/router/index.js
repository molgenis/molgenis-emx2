import { createRouter, createWebHashHistory } from "vue-router";
import DashboardView from "../views/view-dashboard.vue";

const project = "FORCE";

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      name: "dashboard",
      path: "/",
      component: DashboardView,
      meta: {
        title: "Dashboard",
      },
    },
  ],
});

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} | ${project}` : project;
});

export default router;
