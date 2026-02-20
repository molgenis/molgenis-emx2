import { defineNuxtRouteMiddleware, navigateTo } from "#app";
import { getLegacyRedirectTarget } from "../utils/legacyRedirectUtils";

export default defineNuxtRouteMiddleware((to) => {
  const target = getLegacyRedirectTarget(to.path);
  if (target) {
    return navigateTo(target, { redirectCode: 301 });
  }
});
