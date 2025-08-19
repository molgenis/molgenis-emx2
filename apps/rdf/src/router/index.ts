import { createRouter, createWebHashHistory } from "vue-router";
import Shacl from "../views/Shacl.vue";
import Sparql from "../views/Sparql.vue";

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: "/",
      redirect: { path: "/shacl" },
    },
    {
      path: "/shacl",
      component: Shacl,
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
