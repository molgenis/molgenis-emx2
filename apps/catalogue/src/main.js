import Vue from "vue";
import VueRouter from "vue-router";
import App from "./App.vue";
import CatalogueView from "./views/CatalogueView";
import InstitutionView from "./views/InstitutionView";
import DatabankView from "./views/DatabankView";
import TableView from "./views/TableView";
import NetworkView from "./views/NetworkView";
import ReleasesView from "./views/ReleasesView";
import DatasourceView from "./views/DatasourceView";
import ModelView from "./views/ModelView";
import ResourceListView from "./views/ResourceListView";
import AffiliationView from "./views/AffiliationView";
import ContactView from "./views/ContactView";

Vue.config.productionTip = false;

Vue.use(VueRouter);

const router = new VueRouter({
  routes: [
    { name: "Catalogue", path: "/", component: CatalogueView },
    { name: "Cohorts", path: "/alt", component: NetworkView },
    //list views
    {
      name: "list",
      path: "/list/:tableName",
      props: true,
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
      name: "table",
      path: "/tables/:acronym/:version/:tableName",
      component: TableView,
      props: true,
    },
    {
      name: "network",
      path: "/networks/:acronym",
      props: true,
      component: NetworkView,
    },
    {
      name: "affiliation",
      path: "/affiliations/:acronym",
      props: true,
      component: AffiliationView,
    },
    {
      name: "contact",
      path: "/contacts/:name",
      props: true,
      component: ContactView,
    },
  ],
});

new Vue({
  router,
  render: (h) => h(App),
}).$mount("#app");
