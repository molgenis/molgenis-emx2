import Vue from "vue";
import VueRouter from "vue-router";
import App from "./App.vue";
import store from "./store/store";
import CatalogueView from "./views/CatalogueView";
import ResourceDetailsView from "./views/ResourceDetailsView";
import TableView from "./views/TableView";
import NetworkView from "./views/NetworkView";
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
import NetworksHome from "./network/NetworksHome";
import NetworkVariables from "./network/NetworkVariables";
import NetworkCohorts from "./network/NetworkCohorts";
import NetworkDetails from "./network/NetworkDetails";

Vue.config.productionTip = false;

Vue.use(VueRouter);

const router = new VueRouter({
  routes: [
    { name: "Catalogue", path: "/", component: CatalogueView },
    {
      name: "Databanks",
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
      name: "Cohorts",
      path: "/cohorts",
      props: (route) => ({ searchTerm: route.query.q, tableName: "Cohorts" }),
      component: ResourceListView,
    },
    {
      name: "Institutions",
      path: "/institutions",
      props: (route) => ({
        searchTerm: route.query.q,
        tableName: "Institutions",
      }),
      component: ResourceListView,
    },
    {
      name: "Datasources",
      path: "/datasources",
      props: (route) => ({
        searchTerm: route.query.q,
        tableName: "Datasources",
      }),
      component: ResourceListView,
    },
    {
      name: "Networks",
      path: "/networks",
      props: (route) => ({
        searchTerm: route.query.q,
        tableName: "Networks",
      }),
      component: ResourceListView,
    },
    {
      name: "Models",
      path: "/models",
      props: (route) => ({
        searchTerm: route.query.q,
        tableName: "Models",
      }),
      component: ResourceListView,
    },
    {
      name: "SourceDataDictionaries",
      path: "/source-data-dictionaries",
      props: (route) => ({
        searchTerm: route.query.q,
        tableName: "SourceDataDictionaries",
      }),
      component: ResourceListView,
    },
    {
      name: "TargetDataDictionaries",
      path: "/target-data-dictionaries",
      props: (route) => ({
        searchTerm: route.query.q,
        tableName: "TargetDataDictionaries",
      }),
      component: ResourceListView,
    },
    {
      name: "Studies",
      path: "/studies",
      props: (route) => ({
        searchTerm: route.query.q,
        tableName: "Studies",
      }),
      component: ResourceListView,
    },
    {
      name: "SourceVariables",
      path: "/source-variables",
      props: (route) => ({
        searchTerm: route.query.q,
        tableName: "SourceVariables",
      }),
      component: ResourceListView,
    },
    {
      name: "TargetVariables",
      path: "/target-variables",
      props: (route) => ({
        searchTerm: route.query.q,
        tableName: "TargetVariables",
      }),
      component: ResourceListView,
    },
    {
      name: "SourceTables",
      path: "/source-tables",
      props: (route) => ({
        searchTerm: route.query.q,
        tableName: "SourceTables",
      }),
      component: ResourceListView,
    },
    {
      name: "TargetTables",
      path: "/target-tables",
      props: (route) => ({
        searchTerm: route.query.q,
        tableName: "TargetTables",
      }),
      component: ResourceListView,
    },
    {
      name: "TableMappings",
      path: "/table-mappings",
      props: (route) => ({
        searchTerm: route.query.q,
        tableName: "TableMappings",
      }),
      component: ResourceListView,
    },
    {
      name: "VariableMappings",
      path: "/variable-mappings",
      props: (route) => ({
        searchTerm: route.query.q,
        tableName: "VariableMappings",
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
      name: "Cohorts-details",
      path: "/cohorts/:pid",
      component: CohortView,
      props: true,
    },
    {
      path: "/alt-cohorts/:pid",
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
      name: "SourceDataDictionaries-details",
      path: "/source-data-dictionaries/:resource/:version",
      component: ResourceDetailsView,
      props: (route) => ({
        table: "SourceDataDictionaries",
        color: "warning",
        filter: {
          resource: { pid: { equals: route.params.resource } },
          version: { equals: route.params.version },
        },
      }),
    },
    {
      name: "TargetDataDictionaries-details",
      path: "/target-data-dictionaries/:resource/:version",
      component: ResourceDetailsView,
      props: (route) => ({
        table: "TargetDataDictionaries",
        color: "warning",
        filter: {
          resource: { pid: { equals: route.params.resource } },
          version: { equals: route.params.version },
        },
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
      name: "SourceVariables-details",
      path: "/source-variables/:pid/:version/:table/:name",
      props: (route) => ({
        ...route.params,
        tableName: "SourceVariables",
      }),
      component: VariableView,
    },
    {
      name: "TargetVariables-details",
      path: "/target-variables/:pid/:version/:table/:name",
      props: (route) => ({
        ...route.params,
        tableName: "TargetVariables",
      }),
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
      name: "SourceTables-details",
      path: "/source-tables/:pid/:version/:name",
      component: TableView,
      props: (route) => ({
        ...route.params,
        tableName: "SourceTables",
      }),
    },
    {
      name: "TargetTables-details",
      path: "/target-tables/:pid/:version/:name",
      component: TableView,
      props: (route) => ({
        ...route.params,
        tableName: "TargetTables",
      }),
    },
    {
      name: "VariableMappings-details",
      path: "/variable-mappings/:toResource/:toVersion/:toTable/:toVariable/:fromResource/:fromVersion/:fromTable",
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
    {
      // hacky redirects to solve breadcrumb issue
      path: "/cohorts/:cohort/collection-events",
      redirect: "/cohorts/:cohort",
    },
    {
      // hacky redirects to solve breadcrumb issue
      path: "/cohorts/:cohort/subcohorts",
      redirect: "/cohorts/:cohort",
    },
    {
      name: "NetworkLandingPage",
      path: "/networks-catalogue",
      component: NetworksHome,
    },
    {
      name: "NetworkDetails",
      path: "/networks-catalogue/:network",
      props: true,
      component: NetworkDetails,
    },
    {
      name: "NetworkVariables",
      path: "/networks-catalogue/:network/variables",
      props: true,
      component: NetworkVariables,
    },
    {
      name: "NetworkVariableDetailView",
      path: "/networks-catalogue/:network/variables/:name",
      props: (route) => ({ ...route.params, ...route.query }), // both key and value are dynamic
      component: VariableDetailView,
    },
    {
      name: "NetworkCohorts",
      path: "/networks-catalogue/:network/cohorts",
      props: true,
      component: NetworkCohorts,
    },
    {
      name: "NetworkCohortDetailView",
      path: "/networks-catalogue/:network/cohorts/:pid",
      props: true,
      component: CohortView,
    },
    {
      name: "NetworkCohortSubcohort",
      path: "/networks-catalogue/:network/cohorts/:cohort/subcohorts/:name",
      props: true,
      component: Subcohort,
    },
    {
      name: "NetworkCohortCollectionEvent",
      path: "/networks-catalogue/:network/cohorts/:cohort/collection-events/:name",
      props: true,
      component: CollectionEvent,
    },
    {
      // hacky redirects to solve breadcrumb issue
      path: "/networks-catalogue/:network/cohorts/:cohort/collection-events",
      redirect: "/networks-catalogue/:network/cohorts/:cohort",
    },
    {
      // hacky redirects to solve breadcrumb issue
      path: "/networks-catalogue/:network/cohorts/:cohort/subcohorts",
      redirect: "/networks-catalogue/:network/cohorts/:cohort",
    },
  ],
});

new Vue({
  router,
  render: (h) => h(App),
  store,
}).$mount("#app");
