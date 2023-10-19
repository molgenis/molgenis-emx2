import { createRouter, createWebHashHistory } from "vue-router";

import ProviderOverview from "../views/provider-overview.vue";

// Craniosynostosis pages (id: `-cs-`)
import ProviderCsApp from "../views/provider-cs-app.vue";
import ProvidersCsAllGeneral from "../views/provider-cs-all-general.vue";
import ProvidersCsAllSurgical from "../views/provider-cs-all-surgical.vue";
import ProvidersCsCenterGeneral from "../views/provider-cs-center-overview.vue";
import ProvidersCsCenterSurgical from "../views/provider-cs-center-surgical.vue";

// cleft lip and palate pages (id: `-clp-`)
import ProviderClpApp from "../views/provider-clp-app.vue";
import ProviderClpYourCenter from "../views/provider-clp-your-center.vue";
import ProviderClpAllCenters from "../views/provider-clp-all-centers.vue";

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
      component: ProviderOverview,
      meta: {
        title: "Center Overview",
      },
    },

    // router-view for Craniosynostosis (`-cs-`)
    {
      name: "provider-cs",
      path: "/cs",
      component: ProviderCsApp,
      redirect: {
        name: "provider-cs-all-general",
      },
      children: [
        {
          name: "provider-cs-all-general",
          path: "all-centers-general",
          component: ProvidersCsAllGeneral,
          meta: {
            title: "All Center General Overview | Craniosynostosis | ",
          },
        },
        {
          name: "provider-cs-all-surgical",
          path: "all-centers-surgical",
          component: ProvidersCsAllSurgical,
          meta: {
            title: "All Center Surgical Overview | Craniosynostosis | ",
          },
        },
        {
          name: "provider-cs-center-overview",
          path: "center-general",
          component: ProvidersCsCenterGeneral,
          meta: {
            title: "Your Center General Overview | Craniosynostosis | ",
          },
        },
        {
          name: "provider-cs-center-surgical",
          path: "center-surgical",
          component: ProvidersCsCenterSurgical,
          meta: {
            title: "Your center surgical overview | Craniosynostosis",
          },
        },
      ],
    },

    // router-view for Cleft Lip and Palate (ie., -clp-)
    {
      name: "provider-clp",
      path: "/clp",
      component: ProviderClpApp,
      redirect: { name: "provider-clp-your-center" },
      children: [
        {
          name: "provider-clp-your-center",
          path: "center",
          component: ProviderClpYourCenter,
          meta: {
            title: "Your Center | Cleft Lip and Palate | ",
          },
        },
        {
          name: "provider-clp-all-centers",
          path: "all-centers",
          component: ProviderClpAllCenters,
          meta: {
            title: "All Centers | Cleft Lip and Palate | ",
          },
        },
      ],
    },

    // Placeholder for genetic-deafness
    {
      name: "provider-genetic-deafness",
      path: "/genetic-deafness",
      component: ProviderGeneticDeafness,
      meta: {
        title: "Genetic Deafness",
      },
    },

    // Placeholder for larnyxcleft
    {
      name: "provider-larnyxcleft",
      path: "/larnyxcleft",
      component: ProviderLarnyxcleft,
      meta: {
        title: "Larnyxcleft",
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
