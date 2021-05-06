import Vue from "vue"
import VueRouter from "vue-router"
import { BootstrapVue } from 'bootstrap-vue'
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome'
import {
  faCaretRight,
  faExclamationTriangle,
  faSpinner,
  faTimes,
  faFolderOpen,
  faFolder,
  faSearch,
  faCheckCircle,
  faQuestion
} from '@fortawesome/free-solid-svg-icons'
import { library } from '@fortawesome/fontawesome-svg-core'
import draggable from 'vuedraggable'
import hljs from 'highlight.js'
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
import StudiesView from "./views/StudiesView";
import VariableView from "./views/VariableView";
import VariableMappingsView from "./views/VariableMappingsView";
import TableMappingsView from "./views/TableMappingsView";
import store from './store/store'
import BrowseVariablesView from "./views/lifecycle/BrowseVariablesView"
import VariablesView from "./views/lifecycle/VariablesView"
import MappingView from "./views/lifecycle/MappingView"
import MappingDetailView from "./views/lifecycle/MappingDetailView"
import TreeFilter from "./components/lifecycle/TreeFilter"
import 'bootstrap-vue/dist/bootstrap-vue.css'


Vue.config.productionTip = false;

Vue.use(BootstrapVue)
Vue.use(hljs.vuePlugin)
Vue.component('draggable', draggable)
Vue.component('TreeFilter', TreeFilter)
Vue.component('FontAwesomeIcon', FontAwesomeIcon)
library.add(faCaretRight, faExclamationTriangle, faSpinner, faTimes, faFolderOpen, faFolder, faSearch, faCheckCircle, faQuestion)

Vue.use(VueRouter)

const router = new VueRouter({
  linkActiveClass: 'active', // bootstrap 4 active tab class
  routes: [
    { name: "Catalogue", path: "/", component: CatalogueView },
    { name: "Cohorts", path: "/alt", component: NetworkView },
    { 
      path: "/lifecycle", 
      component: BrowseVariablesView,
      children: [
        {
          name: "VariablesView",
          path: 'variables',
          component: VariablesView
        },
        {
          name: "MappingView",
          path: 'mapping',
          component: MappingView
        },
        {
          name: "MappingDetailView",
          path: 'mapping/detail',
          component: MappingDetailView,
          props: true
        },
        { 
          path: '', 
          redirect: '/lifecycle/variables' 
        },
      ]
     },
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
      path:
        "/tablemappings/:fromAcronym/:fromVersion/:fromTable/:toAcronym/:toVersion/:toTable",
      props: true,
      component: TableMappingsView,
    },
  ],
});

new Vue({
  router,
  render: (h) => h(App),
  store
}).$mount("#app");
