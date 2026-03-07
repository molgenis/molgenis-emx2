import { defineNuxtRouteMiddleware, navigateTo } from "#app";
import { useSession } from "../../../tailwind-components/app/composables/useSession";
import { useRoute } from "#app";

export default defineNuxtRouteMiddleware(async () => {
  const route = useRoute();
  const schema = route.params.schema as string;
  const { isAdmin } = await useSession(schema);
  if (!isAdmin.value) {
    return navigateTo("/login");
  }
});
