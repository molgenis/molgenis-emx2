import { createRouter, createWebHashHistory } from "vue-router";

import CranioHome from "../views/view-home.vue";
import AboutPage from "../views/view-about.vue";
import PublicDashboardPage from "../views/view-public-dashboard.vue";

import ProviderHome from "../views/provider-app.vue";
import ProviderOverview from "../views/provider-overview.vue";


// import ProviderCleftLipPalate from "../views/provider-cleft-lip-palate.vue";
import ProviderClpApp from "../views/provider-clp-app.vue";
import ProviderClpYourCenter from "../views/provider-clp-your-center.vue";
import ProviderClpAllCenters from "../views/provider-clp-all-centers.vue";

import ProviderCraniosynostosis from "../views/provider-craniosynostosis.vue";
import ProviderGeneticDeafness from "../views/provider-genetic-deafness.vue";
import ProviderLarnyxcleft from "../views/provider-larnyxcleft.vue";

// For new routes, use the property `meta` to define the document title
// E.g., {..., meta: {title: 'My Page'}}
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
      path: "/providers/:id",
      component: ProviderHome,
      redirect: {
        name: 'provider-overview'
      },
      children: [
        {
          name: "provider-overview",
          path: "",
          component: ProviderOverview,
          meta: {
            title: "Center Overview",
          },
        },
        {
          name: "provider-cranio",
          path: "craniosynostosis",
          component: ProviderCraniosynostosis,
          meta: {
            title: "Craniosynostosis",
          },
        },
        
        // router-view for Cleft Lip and Palate (ie., -clp-)
        {
          name: "provider-clp",
          path: "clp",
          component: ProviderClpApp,
          redirect: { name: 'provider-clp-your-center' },
          children: [
            {
              name: 'provider-clp-your-center',
              path: 'center',
              component: ProviderClpYourCenter,
              meta: {
                title: 'Your Center | Cleft Lip and Palate | '
              }
            },
            {
              name: 'provider-clp-all-centers',
              path: 'all-centers',
              component: ProviderClpAllCenters,
              meta: {
                title: 'All Centers | Cleft Lip and Palate | '
              }
            }
          ]
        },
        {
          name: "provider-genetic-deafness",
          path: "genetic-deafness",
          component: ProviderGeneticDeafness,
          meta: {
            title: "Genetic Deafness",
          },
        },
        {
          name: "provider-larnyxcleft",
          path: "larnyxcleft",
          component: ProviderLarnyxcleft,
          meta: {
            title: "Larnyxcleft",
          },
        },
      ],
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
