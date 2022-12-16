import { createApp } from "vue";
import { createRouter, createWebHashHistory } from "vue-router";
import App from "./App.vue";
import store from "./store/store";
import CatalogueView from "./views/CatalogueView.vue";
import ResourceDetailsView from "./views/ResourceDetailsView.vue";
import DatasetView from "./views/DatasetView.vue";
import ResourceListView from "./views/ResourceListView.vue";
import VariableView from "./views/VariableView.vue";
import VariableMappingsView from "./views/VariableMappingsView.vue";
import DatasetMappingsView from "./views/DatasetMappingsView.vue";
import VariableExplorer from "./views/VariableExplorer.vue";
import VariableDetailView from "./views/VariableDetailView.vue";
import CohortView from "./views/cohorts/CohortView.vue";
import SearchResourceView from "./views/SearchResourceView.vue";
import ResourceRedirectView from "./views/ResourceRedirectView.vue";
import Subcohort from "./views/cohorts/Subcohort.vue";
import CollectionEvent from "./views/cohorts/CollectionEvent.vue";
import NetworksHome from "./network/NetworksHome.vue";
import NetworkVariables from "./network/NetworkVariables.vue";
import NetworkCohorts from "./network/NetworkCohorts.vue";
import NetworkDetails from "./network/NetworkDetails.vue";
import HomeView from "./views/HomeView.vue";

import "molgenis-components/dist/style.css";

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { name: "Catalogue", path: "/", component: CatalogueView },

    { name: "Home", path: "/home", component: HomeView },
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
      name: "Organisations",
      path: "/organisations",
      props: (route) => ({
        searchTerm: route.query.q,
        tableName: "Organisations",
      }),
      component: ResourceListView,
    },
    {
      name: "Datasources",
      path: "/datasources",
      props: (route) => ({
        searchTerm: route.query.q,
        tableName: "Data sources",
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
      name: "Studies",
      path: "/studies",
      props: (route) => ({
        searchTerm: route.query.q,
        tableName: "Studies",
      }),
      component: ResourceListView,
    },
    {
      name: "Variables",
      path: "/variables",
      props: (route) => ({
        searchTerm: route.query.q,
        tableName: "Variables",
      }),
      component: ResourceListView,
    },
    {
      name: "Datasets",
      path: "/datasets",
      props: (route) => ({
        searchTerm: route.query.q,
        tableName: "Datasets",
      }),
      component: ResourceListView,
    },
    {
      name: "DatasetMappings",
      path: "/dataset-mappings",
      props: (route) => ({
        searchTerm: route.query.q,
        tableName: "DatasetMappings",
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
      name: "Organisations-details",
      path: "/organisations/:id",
      component: ResourceDetailsView,
      props: (route) => ({
        table: "Organisations",
        color: "dark",
        filter: { id: { equals: route.params.id } },
      }),
    },
    {
      name: "Networks-details",
      path: "/networks/:id",
      component: ResourceDetailsView,
      props: (route) => ({
        table: "Networks",
        color: "danger",
        filter: { id: { equals: route.params.id } },
      }),
    },
    {
      name: "Cohorts-details",
      path: "/cohorts/:id",
      component: CohortView,
      props: true,
    },
    {
      name: "DataSources-details",
      path: "/datasources/:id",
      component: ResourceDetailsView,
      props: (route) => ({
        table: "Data sources",
        color: "secondary",
        filter: { id: { equals: route.params.id } },
      }),
    },
    {
      name: "Models-details",
      path: "/models/:id",
      component: ResourceDetailsView,
      props: (route) => ({
        table: "Models",
        color: "warning",
        filter: { id: { equals: route.params.id } },
      }),
    },
    {
      name: "Networks-details",
      path: "/networks/:id",
      component: ResourceDetailsView,
      props: (route) => ({
        table: "Networks",
        color: "danger",
        filter: { id: { equals: route.params.id } },
      }),
    },
    {
      name: "Studies-details",
      path: "/studies/:id",
      component: ResourceDetailsView,
      props: (route) => ({
        table: "Studies",
        color: "success",
        filter: { id: { equals: route.params.id } },
      }),
    },
    //variable details
    {
      name: "Variables-details",
      path: "/variables/:id/:dataset/:name",
      props: (route) => ({
        ...route.params,
        tableName: "Variables",
      }),
      component: VariableView,
    },
    {
      name: "Datasets-details",
      path: "/datasets/:id/:name",
      component: DatasetView,
      props: (route) => ({
        ...route.params,
        tableName: "Datasets",
      }),
    },
    {
      name: "VariableMappings-details",
      path:
        "/variable-mappings/:source/:sourceDataset/:target/:targetDataset/:targetVariable",
      props: true,
      component: VariableMappingsView,
    },
    {
      name: "DatasetMappings-detail",
      path: "/dataset-mappings/:source/:sourceDataset/:target/:targetDataset",
      props: true,
      component: DatasetMappingsView,
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
      path: "/networks-catalogue/:network/cohorts/:id",
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
      path:
        "/networks-catalogue/:network/cohorts/:cohort/collection-events/:name",
      props: true,
      component: CollectionEvent,
    },
  ],
});

const app = createApp(App);
app.use(router);
app.use(store);
app.mount("#app");
