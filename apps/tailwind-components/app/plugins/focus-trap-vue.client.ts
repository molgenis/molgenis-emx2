import { FocusTrap } from 'focus-trap-vue'
import { defineNuxtPlugin } from "#app";

type LazyLoadBindingValue = () => Promise<void> | void;

export default defineNuxtPlugin((nuxtApp) => {
  //will fire as about half is shown
  nuxtApp.vueApp.component("focus-trap", FocusTrap);

});