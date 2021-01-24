import Vue from "vue";
import VueRouter from "vue-router";
import App from "./App.vue";
import CatalogueView from "./views/CatalogueView";
import ProvidersListView from "./views/ProvidersListView";
import NetworksListView from "./views/NetworksListView";
import NetworkView from "./views/NetworkView";
import CollectionsListView from "./views/CollectionsListView";
import VariablesListView from "./views/VariablesListView";
import ProviderView from "./views/ProviderView";
import CollectionView from "./views/CollectionView";
import DatasetView from "./views/DatasetView";
import DatasetListView from "./views/DatasetListView";

Vue.config.productionTip = false;

Vue.use(VueRouter);

const router = new VueRouter({
  routes: [
    { name: "Catalogue", path: "/", component: CatalogueView },
    {
      name: "providers",
      path: "/providers",
      component: ProvidersListView,
    },
    {
      name: "provider",
      path: "/providers/:providerAcronym",
      component: ProviderView,
      props: true,
    },
    {
      name: "provider-collection",
      path: "/providers/:providerAcronym/:collectionAcronym",
      component: CollectionView,
      props: true,
    },
    {
      name: "provider-dataset",
      path: "/providers/:providerAcronym/:collectionAcronym/:datasetName",
      component: DatasetView,
      props: true,
    },
    {
      name: "networks",
      path: "/networks",
      component: NetworksListView,
    },
    {
      name: "network",
      path: "/networks/:networkAcronym",
      component: NetworkView,
      props: true,
    },
    {
      name: "network-dataset",
      path: "/networks/:networkAcronym/:datasetName",
      component: DatasetView,
      props: true,
    },
    {
      name: "collections",
      path: "/collections",
      component: CollectionsListView,
    },
    {
      name: "collection",
      path: "/collections/:collectionAcronym",
      component: CollectionView,
      props: true,
    },
    {
      name: "collection-dataset",
      path: "/collections/:collectionAcronym/:datasetName",
      component: DatasetView,
      props: true,
    },
    {
      name: "datasets",
      path: "/datasets",
      component: DatasetListView,
    },
    {
      name: "dataset",
      path: "/datasets/:collectionAcronym/:datasetName",
      component: DatasetView,
      props: true,
    },
    {
      name: "dataset-collection",
      path: "/datasets/:collectionAcronym",
      component: CollectionView,
      props: true,
    },
    {
      name: "dataset-network",
      path: "/datasets-network/:networkAcronym",
      component: NetworkView,
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
