/* Create app when rendering server side; route and state will be injected */
import { createApp } from "./createApp";
import renderVueComponentToString from "vue-server-renderer/basic.js";

const { app, router } = createApp();

//route is passed from environment
console.log("received route: " + route);
router.push({ path: route });

console.log("Current route: " + JSON.stringify(router.currentRoute));

router.onReady(() => {
  console.log("router ready");
  renderVueComponentToString(app, (err, html) => {
    if (err) throw err;
    rendered = String(html);
    console.log("server side rendering complete");
  });
});
