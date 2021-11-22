import { createApp } from "./app";
import renderVueComponentToString from "vue-server-renderer/basic.js";

//create the app
const { app, router } = createApp();

console.log("received route: " + route);

//route is passed from environment
router.push(route);

router.onReady(() => {
  console.log("router ready");
  renderVueComponentToString(app, (err, res) => {
    if (res) {
      rendered = String(res);
    } else {
      console.log("rendering failed: " + err);
      rendered = String(err);
    }
  });
});

console.log("done");
