import Vue from "vue";
import VueRouter from "vue-router";
import App from "./App.vue";
import store from "./store/store";
import CatalogueView from "./views/CatalogueView";
import InstitutionView from "./views/InstitutionView";
import DatabankView from "./views/DatabankView";
import TableView from "./views/TableView";
import NetworkView from "./views/NetworkView";
import ReleasesView from "./views/ReleasesView";
import DatasourceView from "./views/DatasourceView";
import ModelView from "./views/ModelView";
import ResourceListView from "./views/ResourceListView";
import ContactView from "./views/ContactView";
import StudiesView from "./views/StudiesView";
import VariableView from "./views/VariableView";
import VariableMappingsView from "./views/VariableMappingsView";
import TableMappingsView from "./views/TableMappingsView";
import VariableExplorer from "./views/VariableExplorer";
import VariableDetailView from "./views/VariableDetailView";
import CohortView from "./views/CohortView";
import SearchAll from "./views/SearchAll";
import SearchResults from "./views/SearchResults";

Vue.config.productionTip = false;

Vue.use(VueRouter);

const router = new VueRouter({
  routes: [
    { name: "Catalogue", path: "/", component: CatalogueView },
    { name: "SearchAll", path: "/search-all", component: SearchAll },
    {
      name: "SearchResults",
      path: "/search-results",
      props: (route) => ({ query: route.query.q }),
      component: SearchResults,
    },
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
      path: "/institutions/:acronym",
      component: InstitutionView,
      props: true,
    },

    {
      name: "release",
      path: "/releases/:acronym/:version",
      component: ReleasesView,
      props: true,
    },
    {
      name: "databank",
      path: "/databanks/:acronym",
      component: DatabankView,
      props: true,
    },
    {
      name: "cohort",
      path: "/cohorts/:acronym",
      component: CohortView,
      props: true,
    },
    {
      name: "datasource",
      path: "/datasources/:acronym",
      component: DatasourceView,
      props: true,
    },
    {
      name: "model",
      path: "/models/:acronym",
      component: ModelView,
      props: true,
    },
    {
      name: "network",
      path: "/networks/:acronym",
      props: true,
      component: NetworkView,
    },
    {
      name: "contact",
      path: "/contacts/:name",
      props: true,
      component: ContactView,
    },
    {
      name: "studie",
      path: "/studies/:acronym",
      props: true,
      component: StudiesView,
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
