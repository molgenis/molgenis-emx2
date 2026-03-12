import { createApp } from "vue";
import App from "./App.vue";
import router from "./router/index";

import "molgenis-components";
import "molgenis-viz";
import "./styles/index.scss";

const app = createApp(App);
app.use(router);
app.mount("#app");
