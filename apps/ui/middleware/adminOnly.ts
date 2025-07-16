import { defineNuxtRouteMiddleware, navigateTo } from "#app";
import { useSession } from "#imports";

export default defineNuxtRouteMiddleware(async () => {
  const { isAdmin } = await useSession();
  console.log("Admin check in middleware:", isAdmin.value);
  if (!isAdmin.value) {
    return navigateTo("/login");
  }
});
