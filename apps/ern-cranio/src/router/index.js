import { createRouter, createWebHashHistory } from "vue-router";

import CranioHome from "../views/view-home.vue";
import AboutPage from "../views/view-about.vue";
import PublicDashboardPage from "../views/view-public-dashboard.vue";

// For new routes, use the property `meta` to define the document title
// E.g., {..., meta: {title: 'My Page'}}
const project = 'ERN CRANIO';


const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      name: 'home',
      path: '/',
      component: CranioHome
    },
    {
      name: 'about',
      path: '/about',
      component: AboutPage,
      meta: {
        title: 'About'
      }
    },
    {
      name: 'dashboard',
      path: '/dashboard',
      component: PublicDashboardPage,
      meta: {
        title: 'Dashboard'
      }
    }
  ],
  scrollBehavior (to, from, savedPosition) {
    return savedPosition || { top: 0, left: 0 };
  }
});

router.afterEach(to => {
  document.title = to.meta.title ? `${to.meta.title} | ${project}` : project
});

export default router;