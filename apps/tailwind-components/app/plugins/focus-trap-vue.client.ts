import { FocusTrap } from "focus-trap-vue";
import { defineNuxtPlugin } from "#app";

export default defineNuxtPlugin((nuxtApp) => {
  //will fire as about half is shown
  nuxtApp.vueApp.component("focus-trap", FocusTrap);
});
