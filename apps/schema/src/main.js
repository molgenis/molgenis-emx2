import { createApp } from "vue";
import { createRouter, createWebHashHistory } from "vue-router";
import App from "./App.vue";
import Schema from "./components/Schema.vue";
import PrintViewTable from "./components/PrintViewTable.vue";
import PrintViewList from "./components/PrintViewList.vue";
import VueScrollTo from "vue-scrollto";
import YamlEditor from "./components/YamlEditor.vue";

import "molgenis-components/dist/style.css";

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: "/", component: Schema },
    { path: "/print", component: PrintViewTable },
    { path: "/print-list", component: PrintViewList },
    { path: "/yaml", component: YamlEditor },
  ],
});

const app = createApp(App);
app.use(router);
app.use(VueScrollTo);
app.mount("#app");
