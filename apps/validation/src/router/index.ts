import { createRouter, createWebHashHistory } from "vue-router";
import Shacls from "../components/Shacls.vue";
import Table from "../components/Table.vue";

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
      path: "/table",
      component: Table,
      props: true,
      name: "table",
    },
  ],
});

export default router;
