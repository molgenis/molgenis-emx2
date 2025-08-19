import { createRouter, createWebHashHistory } from "vue-router";
import Shacls from "../components/Shacls.vue";
import Sparql from "../components/Sparql.vue";

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: "/",
      redirect: { path: "/shacl" },
    },
    {
      path: "/shacl",
      component: Shacls,
      props: true,
      name: "shacl",
    },
    {
      path: "/sparql",
      component: Sparql,
      props: true,
      name: "sparql",
    },
  ],
});

export default router;
