import { createRouter, createWebHashHistory } from "vue-router";

import HomePage from "../views/home-page.vue";
import organisationsPage from "../views/organisations-page.vue";

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      name: "home",
      path: "/",
      component: HomePage,
    },
    {
      name: "organisations",
      path: "/data/organisations",
      component: organisationsPage,
      meta: {
        title: "Organisation Manager",
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
