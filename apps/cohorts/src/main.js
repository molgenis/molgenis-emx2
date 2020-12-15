import Vue from "vue";
import VueRouter from "vue-router";
import App from "./App.vue";
import VariablesView from "./components/VariablesView";
import CollectionsList from "./components/CollectionsList";
import CollectionDatasets from "./components/CollectionDatasets";
import DatasetDescription from "./components/DatasetDescription";
import DatasetVariables from "./components/DatasetVariables";
import DatasetHarmonisations from "./components/DatasetHarmonisations";
import CollectionVariables from "./components/CollectionVariables";
import CollectionDescription from "./components/CollectionDescription";
import CollectionHarmonisations from "./components/CollectionHarmonisations";

Vue.config.productionTip = false;

Vue.use(VueRouter);

const router = new VueRouter({
  routes: [
    {
      name: "Variables",
      path: "/variables",
      component: VariablesView,
    },
    {
      name: "collections",
      path: "/collections",
      component: CollectionsList,
    },
    {
      name: "collection-datasets",
      path: "/collection-datasets/:collectionAcronym",
      component: CollectionDatasets,
      props: true,
    },
    {
      name: "collection-variables",
      path: "/collection-variables/:collectionAcronym",
      component: CollectionVariables,
      props: true,
    },
    {
      name: "collection-description",
      path: "/collection-description/:collectionAcronym",
      component: CollectionDescription,
      props: true,
    },
    {
      name: "collection-harmonisations",
      path: "/collection-harmonisations/:collectionAcronym",
      component: CollectionHarmonisations,
      props: true,
    },
    {
      name: "dataset-description",
      path: "/dataset-description/:collectionAcronym/:datasetName",
      component: DatasetDescription,
      props: true,
    },
    {
      name: "dataset-variables",
      path: "/dataset-variables/:collectionAcronym/:datasetName",
      component: DatasetVariables,
      props: true,
    },
    {
      name: "dataset-harmonisations",
      path: "/dataset-harmonisations/:collectionAcronym/:datasetName",
      component: DatasetHarmonisations,
      props: true,
    },
    {
      path: "/",
      redirect: "/collections",
    },
  ],
});

new Vue({
  router,
  render: (h) => h(App),
}).$mount("#app");
