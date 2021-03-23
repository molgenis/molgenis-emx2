import AffiliationView from "../views/catalogue/AffiliationView.vue"
import CatalogueView from "../views/catalogue/CatalogueView.vue"
import ContactView from "../views/catalogue/ContactView.vue"
import DatabankView from "../views/catalogue/DatabankView.vue"
import DatasourceView from "../views/catalogue/DatasourceView.vue"
import InstitutionView from "../views/catalogue/InstitutionView.vue"
import ModelView from "../views/catalogue/ModelView.vue"
import NetworkView from "../views/catalogue/NetworkView.vue"
import ReleasesView from "../views/catalogue/ReleasesView.vue"
import ResourceListView from "../views/catalogue/ResourceListView.vue"
import StudiesView from "../views/catalogue/StudiesView.vue"
import TableMappingsView from "../views/catalogue/TableMappingsView.vue"
import TableView from "../views/catalogue/TableView.vue"
import VariableMappingsView from "../views/catalogue/VariableMappingsView.vue"
import VariableView from "../views/catalogue/VariableView.vue"

export default [
    {
        component: CatalogueView,
        name: "Catalogue",
        path: "/",
    },
    {
        component: NetworkView ,
        name: "Cohorts",
        path: "/alt",
    },
    // list views
    {
        component: ResourceListView,
        name: "list",
        path: "/list/:tableName",
        props: true,
    },
    {
        component: InstitutionView,
        name: "institution",
        path: "/institutions/:acronym",
        props: true,
    },

    {
        component: ReleasesView,
        name: "release",
        path: "/releases/:acronym/:version",
        props: true,
    },
    {
        component: DatabankView,
        name: "databank",
        path: "/databanks/:acronym",
        props: true,
    },

    {
        component: DatasourceView,
        name: "datasource",
        path: "/datasources/:acronym",
        props: true,
    },
    {
        component: ModelView,
        name: "model",
        path: "/models/:acronym",
        props: true,
    },
    {
        component: NetworkView,
        name: "network",
        path: "/networks/:acronym",
        props: true,
    },
    {
        component: AffiliationView,
        name: "affiliation",
        path: "/affiliations/:acronym",
        props: true,
    },
    {
        component: ContactView,
        name: "contact",
        path: "/contacts/:name",
        props: true,
    },
    {
        component: StudiesView,
        name: "studie",
        path: "/studies/:acronym",
        props: true,
    },
    {
        component: VariableView,
        name: "variable",
        path: "/variables/:acronym/:version/:table/:name",
        props: true,
    },
    {
        component: TableView,
        name: "table",
        path: "/tables/:acronym/:version/:name",
        props: true,
    },
    {
        component: VariableMappingsView,
        name: "variablemapping",
        path: "/variablemappings/:acronym/:version/:name",
        props: true,
    },
    {
        component: TableMappingsView,
        name: "tablemapping",
        path:
            "/tablemappings/:fromAcronym/:fromVersion/:fromTable/:toAcronym/:toVersion/:toTable",
        props: true,
    },
]