import Vue from "vue";
import VueRouter from "vue-router";
import App from "./App.vue";
import VariablesView from "./components/VariablesView";
import CollectionsList from "./components/CollectionsList";
import CollectionDetails from "./components/CollectionDetails";
import DatasetDescription from "./components/DatasetDescription";
import DatasetVariables from "./components/DatasetVariables";
import DatasetHarmonisations from "./components/DatasetHarmonisations";

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
      name: "collection",
      path: "/collection/:collectionAcronym",
      component: CollectionDetails,
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
