import { createApp } from "vue";

// @ts-ignore
import App from "./App.vue";
// @ts-ignore
import router from "./router/router";

import "molgenis-components";
import "molgenis-viz";

// import "../../molgenis-viz/src/styles/palettes.scss";
// import "../../molgenis-viz/src/styles/variables.scss";
// import "../../molgenis-viz/src/styles/mixins.scss";
// import "./styles/variables.scss";
// import "./styles/index.scss";

const app = createApp(App);
app.use(router);
app.mount("#app");
