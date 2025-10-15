import { defineNuxtRouteMiddleware, navigateTo } from "#app";
import { useSession } from "../../../tailwind-components/app/composables/useSession";

export default defineNuxtRouteMiddleware(async () => {
  const { isAdmin } = await useSession();
  if (!isAdmin.value) {
    return navigateTo("/login");
  }
});
