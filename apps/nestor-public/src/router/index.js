import { createRouter, createWebHashHistory } from "vue-router";

import HomePage from "../pages/view-home.vue";
import AboutPage from "../pages/view-about.vue";
import ContactPage from "../pages/view-contact.vue";
import DisclaimerPage from "../pages/view-disclaimer.vue";
import DocumentsPages from "../pages/view-documents.vue";
import GovernancePage from "../pages/view-governance.vue";
import PrivacyPolicyPage from "../pages/view-privacy.vue";
import PublicDashboard from "../pages/view-dashboard.vue";

const project = "KWF Nestor";

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
      name: "contact",
      path: "/contact",
      component: ContactPage,
      meta: {
        title: "Contact Us",
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
    {
      name: "disclaimer",
      path: "/disclaimer",
      component: DisclaimerPage,
      meta: {
        title: "Disclaimer",
      },
    },
    {
      name: "governance",
      path: "/governance",
      component: GovernancePage,
      meta: {
        title: "Governance",
      },
    },
    {
      name: "privacy",
      path: "/privacy-policy",
      component: PrivacyPolicyPage,
      meta: {
        title: "Privacy Policy",
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
