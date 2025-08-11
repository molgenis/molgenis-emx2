import { defineNuxtRouteMiddleware, navigateTo } from "#app";
import { useSession } from "#imports";

export default defineNuxtRouteMiddleware(async () => {
  const { isAdmin } = await useSession();
  if (!isAdmin.value) {
    return navigateTo("/login");
  }
});
