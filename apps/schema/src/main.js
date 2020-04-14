import Vue from 'vue'
import SchemaDesign from './components/SchemaDesign'
import SchemaView from './components/SchemaView'
import NotFound from './components/NotFound'
import VueRouter from 'vue-router'
import App from "./App";

Vue.config.productionTip = false

const routes = [
    {path: '/:schema/design', name: 'Design', component: SchemaDesign, props: true},
    {path: '/:schema', name: 'Home', component: SchemaView, props: true},
    {path: '*', name: 'NotFound', component: NotFound}]

const router = new VueRouter({
    routes
})

Vue.use(VueRouter)

new Vue({
    router,
    render: h => h(App)
}).$mount("#app");

