import { createRouter, createWebHashHistory } from "vue-router";

import ProviderOverview from "../views/provider-overview.vue";

// Craniosynostosis pages (id: `-cs-`)
import CraniosynostosisAllGeneral from "../views/craniosynostosis/all_centers/general.vue";
import CraniosynostosisAllSurgical from "../views/craniosynostosis/all_centers/surgical.vue";
import CraniosynostosisCenterGeneral from "../views/craniosynostosis/your_center/general.vue";
import CraniosynostosisCenterSurgical from "../views/craniosynostosis/your_center/surgical.vue";

// cleft lip and palate pages (id: `-clp-`)
import CleftLipPalateYourCenter from "../views/cleft_lip_palate/your_center.vue";
import CleftLipPalateLevel1 from "../views/cleft_lip_palate/level_1.vue";

// Facial dysostosis
import FacialDysostosisLevel1 from "../views/facial_dysostosis/level_1.vue";

// Craniofacial microsomia
import CraniofacialMicrosomiaLevel1 from "../views/craniofacial_microsomia/level_1.vue";

// genetic hearing loss
import GeneticHearingLossYourCenter from "../views/genetic_hearing_loss/your_center.vue";
import GeneticHearingLossAllCenters from "../views/genetic_hearing_loss/all_centers.vue";

import ErrorPage from "../views/view-404.vue";
import UnderConstruction from "../views/UnderConstruction.vue";

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
          component: CraniosynostosisAllGeneral,
          meta: {
            title: "All Center General Overview | Craniosynostosis",
          },
        },
        {
          name: "provider-cs-all-surgical",
          path: "all-centers-surgical",
          component: CraniosynostosisAllSurgical,
          meta: {
            title: "All Center Surgical Overview | Craniosynostosis",
          },
        },
        {
          name: "provider-cs-center-overview",
          path: "center-general",
          component: CraniosynostosisCenterGeneral,
          meta: {
            title: "Your Center General Overview | Craniosynostosis",
          },
        },
        {
          name: "provider-cs-center-surgical",
          path: "center-surgical",
          component: CraniosynostosisCenterSurgical,
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
          component: CleftLipPalateYourCenter,
          meta: {
            title: "Your Center | Cleft Lip and Palate",
          },
        },
        {
          name: "provider-clp-level-1",
          path: "level-1",
          component: CleftLipPalateLevel1,
          meta: {
            title: "Level 1 | Cleft Lip and Palate",
          },
        },
        {
          name: "provider-clp-level-2",
          path: "level-2",
          component: UnderConstruction, // TODO: update with level 2 page when available
          meta: {
            title: "Level 2 | Cleft Lip and Palate",
          },
        },
      ],
    },

    // router-view for Facial Dysostosis (ie., -facial-dysostosis-)
    {
      name: "provider-facial-dysostosis",
      path: "/facial-dysostosis",
      redirect: { name: "provider-facial-dysostosis-level-1" },
      children: [
        {
          name: "provider-facial-dysostosis-level-1",
          path: "level-1",
          component: FacialDysostosisLevel1,
          meta: {
            title: "Level 1 | Facial Dysostosis",
          },
        },
        {
          name: "provider-facial-dysostosis-level-2",
          path: "level-2",
          component: UnderConstruction, // TODO: update with level 2 page when available
          meta: {
            title: "Level 2 | Facial Dysostosis",
          },
        },
      ],
    },

    // router-view for Craniofacial Microsomia (ie., -craniofacial-microsomia-)
    {
      name: "provider-craniofacial-microsomia",
      path: "/craniofacial-microsomia",
      redirect: { name: "provider-craniofacial-microsomia-level-1" },
      children: [
        {
          name: "provider-craniofacial-microsomia-level-1",
          path: "level-1",
          component: CraniofacialMicrosomiaLevel1,
          meta: {
            title: "Level 1 | Craniofacial Microsomia",
          },
        },
        {
          name: "provider-craniofacial-microsomia-level-2",
          path: "level-2",
          component: UnderConstruction, // TODO: update with level 2 page when available
          meta: {
            title: "Level 2 | Craniofacial Microsomia",
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
          component: GeneticHearingLossYourCenter,
          meta: {
            title: "Your center | Genetic hearing loss",
          },
        },
        {
          name: "provider-ghl-all-centers",
          path: "all-centers",
          component: GeneticHearingLossAllCenters,
          meta: {
            title: "All centers | Genetic hearing loss",
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
