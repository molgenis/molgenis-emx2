import { createRouter, createWebHashHistory } from "vue-router";

import HomePage from "../views/view-home.vue";
import AboutPage from "../views/view-about.vue";
import ContactPage from "../views/view-contact.vue";
import DisclaimerPage from "../views/view-disclaimer.vue";
import DocumentsPages from "../views/view-documents.vue";
import GovernancePage from "../views/view-governance.vue";
import PrivacyPolicyPage from "../views/view-privacy.vue";
import PublicDashboard from "../views/view-public-dashboard.vue";

const project = "ERN GENTURIS";

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
