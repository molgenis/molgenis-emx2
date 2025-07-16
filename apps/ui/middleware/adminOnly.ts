import { defineNuxtRouteMiddleware, navigateTo } from "#app";
import { useSession } from "#imports";

export default defineNuxtRouteMiddleware(async () => {
  const { sessionPromise, isAdmin } = useSession();
  await sessionPromise.value;
  if (!isAdmin.value) {
    return navigateTo("/login");
  }
});
