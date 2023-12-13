import { createApp } from "vue";
import { createRouter, createWebHashHistory } from "vue-router";
import { EditModal } from "molgenis-components";
import VueScrollTo from "vue-scrollto";

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
import SearchAllResources from "./views/SearchAllResources.vue";

import "molgenis-components/dist/style.css";

const scrollBehavior = (to, from, savedPosition) => {
  return savedPosition || { top: 0, left: 0 };
};
const router = createRouter({
  history: createWebHashHistory(),
  scrollBehavior: scrollBehavior,
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
      name: "search2",
      path: "/search2",
      component: SearchAllResources,
    },
    {
      name: "Cohorts",
      path: "/cohorts",
      props: (route) => ({ searchTerm: route.query.q, tableId: "Cohorts" }),
      component: ResourceListView,
    },
    {
      name: "Organisations",
      path: "/organisations",
      props: (route) => ({
        searchTerm: route.query.q,
        tableId: "Organisations",
      }),
      component: ResourceListView,
    },
    {
      name: "Datasources",
      path: "/datasources",
      props: (route) => ({
        searchTerm: route.query.q,
        tableId: "DataSources",
      }),
      component: ResourceListView,
    },
    {
      name: "Databanks",
      path: "/databanks",
      props: (route) => ({
        searchTerm: route.query.q,
        tableId: "Databanks",
      }),
      component: ResourceListView,
    },
    {
      name: "Networks",
      path: "/networks",
      props: (route) => ({
        searchTerm: route.query.q,
        tableId: "Networks",
      }),
      component: ResourceListView,
    },
    {
      name: "Models",
      path: "/models",
      props: (route) => ({
        searchTerm: route.query.q,
        tableId: "Models",
      }),
      component: ResourceListView,
    },
    {
      name: "Studies",
      path: "/studies",
      props: (route) => ({
        searchTerm: route.query.q,
        tableId: "Studies",
      }),
      component: ResourceListView,
    },
    {
      name: "Variables",
      path: "/variables",
      props: (route) => ({
        searchTerm: route.query.q,
        tableId: "Variables",
      }),
      component: ResourceListView,
    },
    {
      name: "Datasets",
      path: "/datasets",
      props: (route) => ({
        searchTerm: route.query.q,
        tableId: "Datasets",
      }),
      component: ResourceListView,
    },
    {
      name: "DatasetMappings",
      path: "/dataset-mappings",
      props: (route) => ({
        searchTerm: route.query.q,
        tableId: "DatasetMappings",
      }),
      component: ResourceListView,
    },
    {
      name: "VariableMappings",
      path: "/variable-mappings",
      props: (route) => ({
        searchTerm: route.query.q,
        tableId: "VariableMappings",
      }),
      component: ResourceListView,
    },
    {
      name: "Publications",
      path: "/publications",
      props: (route) => ({
        searchTerm: route.query.q,
        tableId: "Publications",
      }),
      component: ResourceListView,
    },
    {
      name: "Resources-details",
      path: "/resources/:id",
      component: ResourceRedirectView,
      props: true,
    },
    {
      name: "ExtendedResources-details",
      path: "/resources/:id",
      component: ResourceRedirectView,
      props: true,
    },
    {
      name: "Organisations-details",
      path: "/organisations/:id",
      component: ResourceDetailsView,
      props: (route) => ({
        tableId: "Organisations",
        color: "dark",
        filter: { id: { equals: route.params.id } },
      }),
    },
    {
      name: "Networks-details",
      path: "/networks/:id",
      component: ResourceDetailsView,
      props: (route) => ({
        tableId: "Networks",
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
        tableId: "DataSources",
        color: "secondary",
        filter: { id: { equals: route.params.id } },
      }),
    },
    {
      name: "Databanks-details",
      path: "/databanks/:id",
      component: ResourceDetailsView,
      props: (route) => ({
        tableId: "Databanks",
        color: "info",
        filter: { id: { equals: route.params.id } },
      }),
    },
    {
      name: "Publications-details",
      path: "/publications/:doi",
      component: ResourceDetailsView,
      props: (route) => ({
        tableId: "Publications",
        color: "secondary",
        filter: { doi: { equals: route.params.doi } },
      }),
    },
    {
      name: "Models-details",
      path: "/models/:id",
      component: ResourceDetailsView,
      props: (route) => ({
        tableId: "Models",
        color: "warning",
        filter: { id: { equals: route.params.id } },
      }),
    },
    {
      name: "Networks-details",
      path: "/networks/:id",
      component: ResourceDetailsView,
      props: (route) => ({
        tableId: "Networks",
        color: "danger",
        filter: { id: { equals: route.params.id } },
      }),
    },
    {
      name: "Studies-details",
      path: "/studies/:id",
      component: ResourceDetailsView,
      props: (route) => ({
        tableId: "Studies",
        color: "success",
        filter: { id: { equals: route.params.id } },
      }),
    },
    {
      name: "Contacts-details",
      path: "/contacts/:resource/:firstName/:lastName",
      component: ResourceDetailsView,
      props: (route) => ({
        tableId: "Contacts",
        color: "success",
        filter: {
          firstName: { equals: route.params.firstName },
          lastName: { equals: route.params.lastName },
          resource: { id: { equals: route.params.resource } },
        },
      }),
    },
    //variable details
    {
      name: "Variables-details",
      path: "/variables/:resource/:dataset/:name",
      props: (route) => ({
        ...route.params,
        tableId: "Variables",
      }),
      component: VariableView,
    },
    {
      name: "Datasets-details",
      path: "/datasets/:resource/:name",
      component: DatasetView,
      props: (route) => ({
        ...route.params,
        tableId: "Datasets",
      }),
    },
    //breadcrumb redirect
    {
      path: "/datasets/:resource",
      component: ResourceRedirectView,
      props: (route) => ({
        id: route.params.resource,
      }),
    },
    {
      name: "VariableMappings-details",
      path: "/variable-mappings/:source/:sourceDataset/:target/:targetDataset/:targetVariable",
      props: true,
      component: VariableMappingsView,
    },
    //redirect breadcrumbs
    {
      path: "/dataset-mappings/:source/:sourceDataset/:target",
      component: ResourceRedirectView,
      props: (route) => ({
        id: route.params.target,
      }),
    },
    {
      path: "/dataset-mappings/:source/:sourceDataset",
      component: DatasetView,
      props: (route) => ({
        resource: route.params.source,
        name: route.params.sourceDataset,
      }),
    },
    {
      path: "/dataset-mappings/:source/:sourceDataset/:target",
      component: ResourceRedirectView,
      props: (route) => ({
        id: route.params.target,
      }),
    },
    {
      name: "DatasetMappings-details",
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
      path: "/networks-catalogue/:network/cohorts/:cohort/collection-events/:name",
      props: true,
      component: CollectionEvent,
    },
  ],
});

const app = createApp(App);
app.use(router);
app.use(store);
app.use(VueScrollTo);

// workaround for not importing recursive component
app.component("EditModal", EditModal);

app.mount("#app");
