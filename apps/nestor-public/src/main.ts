import { createApp } from "vue";

// @ts-expect-error
import App from "./App.vue";
// @ts-expect-error
import router from "./router";

import "molgenis-components";
import "molgenis-viz";
import "./styles/index.scss";

const app = createApp(App);
app.use(router);
app.mount("#app");
