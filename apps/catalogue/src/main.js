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
import VariableView from "./views/VariableView";
import VariableMappingsView from "./views/VariableMappingsView";
import TableMappingsView from "./views/TableMappingsView";
import VariableExplorer from "./views/VariableExplorer";
import VariableDetailView from "./views/VariableDetailView";
import OldCohortView from "./views/cohorts/OldCohortView";
import CohortView from "./views/cohorts/CohortView";
import SearchResourceView from "./views/SearchResourceView";
import ResourceRedirectView from "./views/ResourceRedirectView";
import Subcohort from "./views/cohorts/Subcohort";
import CollectionEvent from "./views/cohorts/CollectionEvent";

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
      name: "Institutions-details",
      path: "/institutions/:pid",
      component: ResourceDetailsView,
      props: (route) => ({
        table: "Institutions",
        color: "dark",
        filter: { pid: { equals: route.params.pid } },
      }),
    },
    {
      name: "Resources-details",
      path: "/resources/:pid",
      component: ResourceRedirectView,
      props: (route) => ({
        table: "Resources",
        color: "dark",
        filter: { pid: { equals: route.params.pid } },
      }),
    },
    {
      name: "Networks-details",
      path: "/networks/:pid",
      component: ResourceDetailsView,
      props: (route) => ({
        table: "Networks",
        color: "danger",
        filter: { pid: { equals: route.params.pid } },
      }),
    },
    {
      path: "/releases/:pid",
      redirect: "/resources/:pid",
    },
    {
      name: "Releases-details",
      path: "/releases/:pid/:version",
      component: ReleasesView,
      props: true,
    },
    {
      name: "Databanks-details",
      path: "/databanks/:pid",
      component: ResourceDetailsView,
      props: (route) => ({
        table: "Databanks",
        color: "info",
        filter: { pid: { equals: route.params.pid } },
      }),
    },
    {
      path: "/alt-cohorts/:pid",
      component: CohortView,
      props: true,
    },
    {
      name: "Cohorts-details",
      path: "/cohorts/:pid",
      component: OldCohortView,
      props: true,
    },
    {
      name: "Datasources-details",
      path: "/datasources/:pid",
      component: ResourceDetailsView,
      props: (route) => ({
        table: "Datasources",
        color: "secondary",
        filter: { pid: { equals: route.params.pid } },
      }),
    },
    {
      name: "Models-details",
      path: "/models/:pid",
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
      name: "Contacts-details",
      path: "/contacts/:name",
      component: ResourceDetailsView,
      props: (route) => ({
        table: "Contacts",
        color: "dark",
        filter: { name: { equals: route.params.name } },
      }),
    },
    {
      name: "Studies-details",
      path: "/studies/:pid",
      component: ResourceDetailsView,
      props: (route) => ({
        table: "Studies",
        color: "success",
        filter: { pid: { equals: route.params.pid } },
      }),
    },
    //make bread crumb work for variable details
    {
      path: "/variables/:pid",
      redirect: "/resources/:pid",
    },
    {
      path: "/variables/:pid/:version",
      redirect: "/releases/:pid/:version",
    },
    {
      path: "/variables/:pid/:version/:table",
      redirect: "/tables/:pid/:version/:table",
    },
    //variable details
    {
      name: "Variables-details",
      path: "/variables/:pid/:version/:table/:name",
      props: true,
      component: VariableView,
    },
    //make bread crumb work for table-details
    {
      path: "/tables/:pid",
      redirect: "/resources/:pid",
    },
    {
      path: "/tables/:pid/:version",
      redirect: "/releases/:pid/:version",
    },
    {
      name: "Tables-details",
      path: "/tables/:pid/:version/:name",
      component: TableView,
      props: true,
    },
    {
      name: "variablemapping",
      path: "/variablemappings/:pid/:version/:name",
      props: true,
      component: VariableMappingsView,
    },
    {
      name: "tablemapping",
      path: "/tablemappings/:fromPid/:fromVersion/:fromTable/:toPid/:toVersion/:toTable",
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
    {
      name: "Subcohort",
      path: "/cohorts/:cohort/subcohorts/:name",
      props: true,
      component: Subcohort,
    },
    {
      name: "CollectionEvent",
      path: "/cohorts/:cohort/collection-events/:name",
      props: true,
      component: CollectionEvent,
    },
  ],
});

new Vue({
  router,
  render: (h) => h(App),
  store,
}).$mount("#app");
