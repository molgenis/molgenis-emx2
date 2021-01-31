import Vue from "vue";
import VueRouter from "vue-router";
import App from "./App.vue";
import CatalogueView from "./views/CatalogueView";
import ProvidersListView from "./views/ProvidersListView";
import ConsortiumListView from "./views/ConsortiumListView";
import ConsortiumView from "./views/ConsortiumView";
import DatabankListView from "./views/DatabankListView";
import VariablesListView from "./views/VariablesListView";
import ProviderView from "./views/ProviderView";
import DatabankView from "./views/DatabankView";
import TableView from "./views/TableView";
import TableListView from "./views/TableListView";

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
      name: "provider-databank",
      path: "/providers/:providerAcronym/:databankAcronym",
      component: DatabankView,
      props: true,
    },
    {
      name: "provider-table",
      path: "/providers/:providerAcronym/:databankAcronym/:tableName",
      component: TableView,
      props: true,
    },
    {
      name: "consortia",
      path: "/consortia",
      component: ConsortiumListView,
    },
    {
      name: "consortium",
      path: "/consortia/:consortiumAcronym",
      component: ConsortiumView,
      props: true,
    },
    {
      name: "consortium-table",
      path: "/consortia/:consortiumAcronym/:tableName",
      component: TableView,
      props: true,
    },
    {
      name: "databanks",
      path: "/databanks",
      component: DatabankListView,
    },
    {
      name: "databank",
      path: "/databanks/:databankAcronym",
      component: DatabankView,
      props: true,
    },
    {
      name: "databank-table",
      path: "/databanks/:databankAcronym/:tableName",
      component: TableView,
      props: true,
    },
    {
      name: "tables",
      path: "/tables",
      component: TableListView,
    },
    {
      name: "table",
      path: "/tables/:databankAcronym/:tableName",
      component: TableView,
      props: true,
    },
    {
      name: "table-databank",
      path: "/tables/:databankAcronym",
      component: DatabankView,
      props: true,
    },
    {
      name: "tables-consortium",
      path: "/tables-consortium/:consortiumAcronym",
      component: ConsortiumView,
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
