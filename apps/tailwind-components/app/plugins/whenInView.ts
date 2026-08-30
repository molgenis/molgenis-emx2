import { defineNuxtPlugin } from "#app";
import { whenInView } from "../directives/whenInView";

export default defineNuxtPlugin((nuxtApp) => {
  nuxtApp.vueApp.directive("when-in-view", whenInView);
});
