import Vue from "vue";
import VueRouter from "vue-router";
import App from "./App.vue";
import store from "./store/store";
import CatalogueView from "./views/CatalogueView";
import ResourceDetailsView from "./views/ResourceDetailsView";
import TableView from "./views/TableView";
import NetworkView from "./views/NetworkView";
import ReleasesView from "./views/ReleasesView";
import ResourceListView from "./views/ResourceListView";
import ContactView from "./views/ContactView";
import VariableView from "./views/VariableView";
import VariableMappingsView from "./views/VariableMappingsView";
import TableMappingsView from "./views/TableMappingsView";
import VariableExplorer from "./views/VariableExplorer";
import VariableDetailView from "./views/VariableDetailView";
import CohortView from "./views/CohortView";

Vue.config.productionTip = false;

Vue.use(VueRouter);

const router = new VueRouter({
  routes: [
    { name: "Catalogue", path: "/", component: CatalogueView },
    { name: "Cohorts", path: "/alt", component: NetworkView },
    {
      name: "databanks",
      path: "/databanks",
      props: { tableName: "Databanks" },
      component: ResourceListView,
    },
    {
      name: "cohorts",
      path: "/cohorts",
      props: { tableName: "Cohorts" },
      component: ResourceListView,
    },
    {
      name: "institutions",
      path: "/institutions",
      props: { tableName: "Institutions" },
      component: ResourceListView,
    },
    {
      name: "datasources",
      path: "/datasources",
      props: { tableName: "Datasources" },
      component: ResourceListView,
    },
    {
      name: "networks",
      path: "/networks",
      props: { tableName: "Networks" },
      component: ResourceListView,
    },
    {
      name: "models",
      path: "/models",
      props: { tableName: "Models" },
      component: ResourceListView,
    },
    {
      name: "studies",
      path: "/studies",
      props: { tableName: "Studies" },
      component: ResourceListView,
    },
    {
      name: "releases",
      path: "/releases",
      props: { tableName: "Releases" },
      component: ResourceListView,
    },
    {
      path: "/releases/:acronym",
      redirect: "/releases",
    },
    {
      name: "variables",
      path: "/variables",
      props: { tableName: "Variables" },
      component: ResourceListView,
    },
    {
      name: "tables",
      path: "/tables",
      props: { tableName: "Tables" },
      component: ResourceListView,
    },
    {
      name: "tablemappings",
      path: "/tablemappings",
      props: { tableName: "TableMappings" },
      component: ResourceListView,
    },
    {
      name: "variablemappings",
      path: "/variablesmappings",
      props: { tableName: "VariableMappings" },
      component: ResourceListView,
    },
    {
      name: "institution",
      path: "/institutions/:pid",
      component: ResourceDetailsView,
      props: (route) => ({
        table: "Institutions",
        color: "dark",
        filter: { pid: { equals: route.params.pid } },
      }),
    },

    {
      name: "release",
      path: "/releases/:acronym/:version",
      component: ReleasesView,
      props: true,
    },
    {
      name: "databank",
      path: "/databanks/:pid",
      component: ResourceDetailsView,
      props: (route) => ({
        table: "Databanks",
        color: "info",
        filter: { pid: { equals: route.params.pid } },
      }),
    },
    {
      name: "cohort",
      path: "/cohorts/:acronym",
      component: CohortView,
      props: true,
    },
    {
      name: "datasource",
      path: "/datasources/:pid",
      component: ResourceDetailsView,
      props: (route) => ({
        table: "Datasources",
        color: "secondary",
        filter: { pid: { equals: route.params.pid } },
      }),
    },
    {
      name: "model",
      path: "/models/:acronym",
      component: ResourceDetailsView,
      props: (route) => ({
        table: "Models",
        color: "warning",
        filter: { pid: { equals: route.params.pid } },
      }),
    },
    {
      name: "network",
      path: "/networks/:pid",
      component: ResourceDetailsView,
      props: (route) => ({
        table: "Networks",
        color: "danger",
        filter: { pid: { equals: route.params.pid } },
      }),
    },
    {
      name: "contact",
      path: "/contacts/:name",
      props: true,
      component: ContactView,
    },
    {
      name: "studie",
      path: "/studies/:pid",
      component: ResourceDetailsView,
      props: (route) => ({
        table: "Studies",
        color: "success",
        filter: { pid: { equals: route.params.pid } },
      }),
    },
    {
      name: "variable",
      path: "/variables/:acronym/:version/:table/:name",
      props: true,
      component: VariableView,
    },
    {
      name: "table",
      path: "/tables/:acronym/:version/:name",
      component: TableView,
      props: true,
    },
    {
      name: "variablemapping",
      path: "/variablemappings/:acronym/:version/:name",
      props: true,
      component: VariableMappingsView,
    },
    {
      name: "tablemapping",
      path: "/tablemappings/:fromAcronym/:fromVersion/:fromTable/:toAcronym/:toVersion/:toTable",
      props: true,
      component: TableMappingsView,
    },
    {
      name: "variableExplorer",
      path: "/variable-explorer",
      props: true,
      component: VariableExplorer,
    },
    {
      name: "VariableDetailView",
      path: "/variable-explorer/:name",
      props: (route) => ({ ...route.params, ...route.query }), // both key and value are dynamic
      component: VariableDetailView,
    },
  ],
});

new Vue({
  router,
  render: (h) => h(App),
  store,
}).$mount("#app");
