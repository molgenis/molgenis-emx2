import { createApp } from "vue";
import { createPinia } from "pinia";
import VueGtag from "vue-gtag";
//@ts-expect-error
import VueMatomo from "vue-matomo/src/index.js";

import App from "./App.vue";
import router from "./router";

import "molgenis-components/dist/style.css";
import "./dev-assets/mg-bbmri-eric-4.css";

/** Add font awesome icons */
import "@fortawesome/fontawesome-free/css/all.css";
import "@fortawesome/fontawesome-free/js/all.js";

/** Add bootstrap icons */
import "bootstrap-icons/font/bootstrap-icons.css";

export const app = createApp(App);

app.use(createPinia());
app.use(router);
app.use(VueGtag, { bootstrap: false }, router);
app.use(VueMatomo, {
  host: "https://analytics.molgeniscloud.org",
  siteId: 2,
  router,
});

app.mount("#app");

declare global {
  interface Window {
    _paq: any[];
  }
}

window._paq.push(["trackPageView"]);
