import { createApp } from "vue";

// @ts-expect-error
import App from "./App.vue";
// @ts-expect-error
import router from "./router";

import "molgenis-components/dist/style.css";
import "molgenis-viz/dist/style.css";
import "./styles/index.scss";

const app = createApp(App);
app.use(router);
app.mount("#app");
