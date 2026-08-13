import { createApp } from "vue";

// @ts-ignore
import App from "./App.vue";
// @ts-ignore
import router from "./router/router";

import "molgenis/molgenis-components.css";
import "vizdist/molgenis-viz.css";

import { RouteLocationNormalizedGeneric } from "vue-router";

const app = createApp(App);

/* Add manual page view tracking for Google Analytics, since the app is a SPA and GA doesn't track page views automatically
 * We already have the cookiebanner which loads GTAG. So we can just add it here after each route change.
 */
router.afterEach((to: RouteLocationNormalizedGeneric) => {
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
