import { createApp } from "vue";
import App from "./App.vue";
import router from "./router";

import "molgenis/molgenis-components.css";
import "vizdist/molgenis-viz.css";

const app = createApp(App);
app.use(router);
app.mount("#app");
