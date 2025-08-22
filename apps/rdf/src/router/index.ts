import { createRouter, createWebHashHistory } from "vue-router";
import Shacl from "../views/Shacl.vue";

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
  ],
});

export default router;
