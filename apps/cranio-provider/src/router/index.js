import { createRouter, createWebHashHistory } from "vue-router";

import ProviderOverview from "../views/provider-overview.vue";

// Craniosynostosis pages (id: `-cs-`)
import ProvidersCsAllGeneral from "../views/cs-all-general.vue";
import ProvidersCsAllSurgical from "../views/cs-all-surgical.vue";
import ProvidersCsCenterGeneral from "../views/cs-center-general.vue";
import ProvidersCsCenterSurgical from "../views/cs-center-surgical.vue";

// cleft lip and palate pages (id: `-clp-`)
import ProviderClpYourCenter from "../views/clp-your-center.vue";
import ProviderClpAllCenters from "../views/clp-all-centers.vue";

// genetic hearing loss
import ProviderGhlYourCenter from "../views/genetic_hearing_loss/YourCenter.vue";
import ProviderGhlAllCenters from "../views/genetic_hearing_loss/AllCenters.vue";

import ErrorPage from "../views/view-404.vue";

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
      redirect: {
        name: "provider-ghl-your-center",
      },
      children: [
        {
          name: "provider-ghl-your-center",
          path: "center",
          component: ProviderGhlYourCenter,
          meta: {
            title: "Your center | Genetic hearing loss | ",
          },
        },
        {
          name: "provider-ghl-all-centers",
          path: "all-centers",
          component: ProviderGhlAllCenters,
          meta: {
            title: "All centers | Genetic hearing loss | ",
          },
        },
      ],
    },

    // error
    {
      name: "404",
      path: "/:pathMatch(.*)*",
      component: ErrorPage,
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
