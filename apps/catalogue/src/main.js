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
import SearchResourceView from "./views/SearchResourceView";

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
      name: "search",
      path: "/search",
      props: (route) => ({ resourceType: route.query.type }),
      component: SearchResourceView,
    },
    {
      name: "cohorts",
      path: "/cohorts",
      props: (route) => ({ searchTerm: route.query.q, tableName: "Cohorts" }),
      component: ResourceListView,
    },
    {
      name: "institutions",
      path: "/institutions",
      props: (route) => ({
        searchTerm: route.query.q,
        tableName: "Institutions",
      }),
      component: ResourceListView,
    },
    {
      name: "datasources",
      path: "/datasources",
      props: (route) => ({
        searchTerm: route.query.q,
        tableName: "Datasources",
      }),
      component: ResourceListView,
    },
    {
      name: "networks",
      path: "/networks",
      props: (route) => ({
        searchTerm: route.query.q,
        tableName: "Networks",
      }),
      component: ResourceListView,
    },
    {
      name: "models",
      path: "/models",
      props: (route) => ({
        searchTerm: route.query.q,
        tableName: "Models",
      }),
      component: ResourceListView,
    },
    {
      name: "studies",
      path: "/studies",
      props: (route) => ({
        searchTerm: route.query.q,
        tableName: "Studies",
      }),
      component: ResourceListView,
    },
    {
      name: "releases",
      path: "/releases",
      props: (route) => ({
        searchTerm: route.query.q,
        tableName: "Releases",
      }),
      component: ResourceListView,
    },
    {
      path: "/releases/:acronym",
      redirect: "/releases",
    },
    {
      name: "variables",
      path: "/variables",
      props: (route) => ({
        searchTerm: route.query.q,
        tableName: "Variables",
      }),
      component: ResourceListView,
    },
    {
      name: "tables",
      path: "/tables",
      props: (route) => ({
        searchTerm: route.query.q,
        tableName: "Tables",
      }),
      component: ResourceListView,
    },
    {
      name: "tablemappings",
      path: "/tablemappings",
      props: (route) => ({
        searchTerm: route.query.q,
        tableName: "TableMappings",
      }),
      component: ResourceListView,
    },
    {
      name: "variablemappings",
      path: "/variablesmappings",
      props: (route) => ({
        searchTerm: route.query.q,
        tableName: "Variablemappings",
      }),
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
      path: "/cohorts/:pid",
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
