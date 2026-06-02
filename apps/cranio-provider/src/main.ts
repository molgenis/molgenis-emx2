import { createApp } from "vue";
import App from "./App.vue";

// @ts-ignore
import router from "./router";

import "../../molgenis-components/dist/molgenis-components.css";
import "molgenis-viz";

const app = createApp(App);
app.use(router);
app.mount("#app");
