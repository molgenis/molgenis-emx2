import { createApp } from "vue";
import App from "./App.vue";
import router from "./router/index";

import "molgenis-components";
<<<<<<< HEAD
=======
import "molgenis-viz";
import "./styles/index.scss";
>>>>>>> 5062ba0cc (chore: update vite to v6)

const app = createApp(App);
app.use(router);
app.mount("#app");
