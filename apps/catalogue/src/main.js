import Vue from "vue";
import VueRouter from "vue-router";
import App from "./App.vue";
import CatalogueView from "./views/CatalogueView";
import InstitutionListView from "./views/InstitutionListView";
import ConsortiumListView from "./views/ProjectListView";
import ConsortiumView from "./views/ProjectView";
import DatabankListView from "./views/DatabankListView";
import VariablesListView from "./views/VariablesListView";
import InstitutionView from "./views/InstitutionView";
import DatabankView from "./views/DatabankView";
import TableView from "./views/TableView";
import TableListView from "./views/TableListView";
import NetworkView from "./views/NetworkView";
import ReleasesListView from "./views/ReleasesListView";
import ReleasesView from "./views/ReleasesView";

Vue.config.productionTip = false;

Vue.use(VueRouter);

const router = new VueRouter({
  routes: [
    { name: "Catalogue", path: "/alt", component: CatalogueView },
    { name: "Cohorts", path: "/", component: NetworkView },
    {
      name: "institutions",
      path: "/institutions",
      component: InstitutionListView,
    },
    {
      name: "institution",
      path: "/institutions/:institutionAcronym",
      component: InstitutionView,
      props: true,
    },
    {
      name: "institution-databank",
      path: "/institutions/:institutionAcronym/:databankAcronym",
      component: DatabankView,
      props: true,
    },
    {
      name: "institution-table",
      path: "/institutions/:institutionAcronym/:databankAcronym/:tableName",
      component: TableView,
      props: true,
    },
    {
      name: "releases",
      path: "/releases",
      component: ReleasesListView,
    },
    {
      name: "release",
      path: "/releases/:resourceAcronym/:version",
      component: ReleasesView,
      props: true,
    },
    {
      name: "projects",
      path: "/projects",
      component: ConsortiumListView,
    },
    {
      name: "project",
      path: "/projects/:projectAcronym",
      component: ConsortiumView,
      props: true,
    },
    {
      name: "project-table",
      path: "/projects/:projectAcronym/:tableName",
      component: TableView,
      props: true,
    },
    {
      name: "databanks",
      path: "/databanks",
      component: DatabankListView,
    },
    {
      name: "databank",
      path: "/databanks/:databankAcronym",
      component: DatabankView,
      props: true,
    },
    {
      name: "databank-table",
      path: "/databanks/:databankAcronym/:tableName",
      component: TableView,
      props: true,
    },
    {
      name: "tables",
      path: "/tables",
      component: TableListView,
    },
    {
      name: "table",
      path: "/tables/:resourceAcronym/:tableName",
      component: TableView,
      props: true,
    },
    {
      name: "table-databank",
      path: "/tables/:databankAcronym",
      component: DatabankView,
      props: true,
    },
    {
      name: "tables-project",
      path: "/tables-project/:projectAcronym",
      component: ConsortiumView,
      props: true,
    },
    {
      name: "variables",
      path: "/variables",
      component: VariablesListView,
    },
  ],
});

new Vue({
  router,
  render: (h) => h(App),
}).$mount("#app");
