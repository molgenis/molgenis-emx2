import { defineNuxtRouteMiddleware, navigateTo } from "#app";
import { useSession } from "#imports";
import { computed } from "vue";

export default defineNuxtRouteMiddleware(async () => {
  const { session } = await useSession();
  const isAdmin = computed(() => session.value?.email === "admin");
  if (!isAdmin.value) {
    return navigateTo("/login");
  }
});
