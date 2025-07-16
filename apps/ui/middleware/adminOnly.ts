import { defineNuxtRouteMiddleware, navigateTo } from "#app";
import { useSession } from "#imports";

export default defineNuxtRouteMiddleware(async () => {
  const { sessionLoaded, isAdmin } = useSession();
  if (sessionLoaded.value && !isAdmin.value) {
    return navigateTo("/login");
  }
});
