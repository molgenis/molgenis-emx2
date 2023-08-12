import { createApp } from "vue";
import { createPinia } from "pinia";

import App from "./App.vue";
import router from "./router";

import "molgenis-components/dist/style.css";

/** When in devmode use this stylesheet */
if (import.meta.env.DEV) {
  import("./dev-assets/mg-bbmri-eric-4.css");
}
/** else???? emx2 does have also css but probably this one is too specific anyway? */
{
  import("./dev-assets/mg-bbmri-eric-4.css");
}

/** Add font awesome icons */
import "@fortawesome/fontawesome-free/css/all.css";
import "@fortawesome/fontawesome-free/js/all.js";

export const app = createApp(App);

app.use(createPinia());
app.use(router);

app.mount("#app");
