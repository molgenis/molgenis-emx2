import { createRouter, createWebHashHistory } from "vue-router";

import AppRoot from "../views/root-page.vue";

import FdhApp from "../views/fdhub/App.vue";
import FdhHomePage from "../views/fdhub/home-page.vue";
import FdhOrganisationsPage from "../views/fdhub/organisations-page.vue";

import ErnReconnetApp from "../views/ern-reconnet/App.vue";
import ErnReconnetHome from "../views/ern-reconnet/home-page.vue";
import ErnReconnetAbout from "../views/ern-reconnet/about-page.vue";
import ErnReconnetDashboard from "../views/ern-reconnet/dashboard-page.vue";

import GdiApp from "../views/gportal/App.vue";
import GdiHome from "../views/gportal/home-page.vue";
import GdiDatasetSearch from "../views/gportal/dataset-search.vue";
import GdiBeaconSearch from "../views/gportal/beacon-search.vue";

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      name: "root",
      path: "/",
      component: AppRoot,
      meta: {
        title: "Site Map",
      },
    },

    // Routes for ERN-RECONNET
    {
      name: "ern-reconnet",
      path: "/ern-reconnet",
      component: ErnReconnetApp,
      children: [
        {
          name: "ern-reconnet-home",
          path: "",
          component: ErnReconnetHome,
          meta: {
            project: "ERN RECONNET",
          },
        },
        {
          name: "ern-reconnet-about",
          path: "about",
          component: ErnReconnetAbout,
          meta: {
            title: "About",
            project: "ERN RECONNET",
          },
        },
        {
          name: "ern-reconnet-dashboard",
          path: "dashboard",
          component: ErnReconnetDashboard,
          meta: {
            title: "Dashboard",
            project: "ERN RECONNET",
          },
        },
      ],
    },
    
    // routes for the GDI Portal
    {
      name: "gportal",
      path: "/gportal",
      component: GdiApp,
      children: [
        {
          name: "gportal-home",
          path: "",
          component: GdiHome,
          meta: {
            project: "PortalGDI"
          }
        },
        {
          name: "gportal-dataset",
          path: "dataset",
          component: GdiDatasetSearch,
          meta: {
            title: "Dataset",
            project: "PortalGDI"
          }
        },
        {
          name: "gportal-beacon",
          path: "beacon",
          component: GdiBeaconSearch,
          meta: {
            title: "Beacon",
            project: "PortalGDI"
          }
        },
        
      ]
    },

    // Routes for FairDataHub tools
    {
      name: "fdhub",
      path: "/fdhub",
      component: FdhApp,
      children: [
        {
          name: "fdhub-home",
          path: "",
          component: FdhHomePage,
          meta: {
            project: "FairDataHub",
          },
        },
        {
          name: "fdhub-organisations",
          path: "organisations",
          component: FdhOrganisationsPage,
          meta: {
            title: "Organisation Manager",
            project: "FairDataHub",
          },
        },
      ],
    },
  ],
  scrollBehavior(to, from, savedPosition) {
    return savedPosition || { top: 0 };
  },
});

router.afterEach((to) => {
  document.title =
    to.meta.title && to.meta.project
      ? `${to.meta.title} | ${to.meta.project}`
      : to.meta.title || to.meta.project;
});

export default router;
