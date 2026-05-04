import jsonWorker from "monaco-editor/esm/vs/language/json/json.worker?worker";
import { createPinia } from "pinia";
import { createApp } from "vue";

import App from "./App.vue";
import router from "./router";

import "../../molgenis-components/dist/molgenis-components.css";
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

/* Add manual page view tracking for Google Analytics, since the app is a SPA and GA doesn't track page views automatically
 * We already have the cookiebanner which loads GTAG. So we can just add it here after each route change.
 */
router.afterEach((to) => {
  // @ts-ignore
  if (window.gtag) {
    // @ts-ignore
    window.gtag("event", "page_view", {
      page_title: document.title,
      page_location: window.location.origin + to.fullPath,
      page_path: to.fullPath,
    });
  }
});

app.use(router);
app.mount("#app");

// Used by Matomo for tracking events
declare global {
  interface Window {
    _paq: any[];
  }
}
