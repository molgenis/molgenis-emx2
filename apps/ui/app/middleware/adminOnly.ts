import { defineNuxtRouteMiddleware, navigateTo, useRoute } from "nuxt/app";
import { useSession } from "../../../tailwind-components/app/composables/useSession";

export default defineNuxtRouteMiddleware(async () => {
  const route = useRoute();
  const schema = route.params.schema as string;
  const { isAdmin } = await useSession(schema);
  if (!isAdmin.value) {
    return navigateTo("/login");
  }
});
