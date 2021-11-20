import { createApp } from "./app";
import renderVueComponentToString from "vue-server-renderer/basic.js";

const { app, router } = createApp();

//route is passed from environment
router.push(route);

router.onReady(() => {
  renderVueComponentToString(app, (err, res) => {
    rendered = String(res);
  });
});
