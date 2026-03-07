import jsonWorker from "monaco-editor/esm/vs/language/json/json.worker?worker";
import { createPinia } from "pinia";
import { createApp } from "vue";
import VueGtag from "vue-gtag";

import App from "./App.vue";
import router from "./router";

import "molgenis-components/dist/style.css";
import "./dev-assets/mg-bbmri-eric-4.css";

/** Add font awesome icons */
import "@fortawesome/fontawesome-free/css/all.css";
import "@fortawesome/fontawesome-free/js/all.js";

/** Add bootstrap icons */
import "bootstrap-icons/font/bootstrap-icons.css";

// @ts-ignore
self.MonacoEnvironment = {
  getWorker(_: any, _label: string) {
    return new jsonWorker();
  },
};
export const app = createApp(App);

app.use(createPinia());
app.use(router);
app.use(VueGtag, { bootstrap: false }, router);

app.mount("#app");

// Used by Matomo for tracking events
declare global {
  interface Window {
    _paq: any[];
  }
}
