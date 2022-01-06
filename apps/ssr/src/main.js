import { createApp } from "./createApp";

/* Used for dev only*/
const { app, router } = createApp();
app.$mount("#app", true);
