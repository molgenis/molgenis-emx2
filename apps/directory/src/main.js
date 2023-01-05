import { createApp } from "vue";
import { createPinia } from "pinia";

import App from "./App.vue";
import router from "./router";

/** When in devmode use this stylesheet */
if (import.meta.env.DEV) {
  import("./dev-assets/mg-bbmri-eric-4.css");
}

/** Add font awesome icons */
import "@fortawesome/fontawesome-free/css/all.css";

const app = createApp(App);

app.use(createPinia());
app.use(router);

app.mount("#app");
