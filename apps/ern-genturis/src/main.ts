import { createApp } from "vue";

// @ts-ignore
import App from "./App.vue";
// @ts-ignore
import router from "./router/router";

import "molgenis-components/dist/style.css";
import "molgenis-viz/dist/style.css";
import "./styles/index.scss";

const app = createApp(App);
app.use(router);
app.mount("#app");
