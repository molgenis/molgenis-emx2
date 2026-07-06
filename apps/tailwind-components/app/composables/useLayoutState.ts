import { computed, ref, watch } from "vue";
import { useRoute } from "#app/composables/router";
import { useSession } from "../composables/useSession";
import { useLogo } from "../composables/useLogo";
import { useLayoutHead } from "./useLayoutHead";
import { useLayoutMenu } from "./useLayoutMenu";

export async function useLayoutState() {
  const route = useRoute();
  const schema = computed(() => route.params.schema as string);
  const { session, signOut } = await useSession(schema.value);
  const logoUrl = ref<string | undefined>(undefined);

  watch(
    () => route.params.schema,
    async (currentSchema) => {
      logoUrl.value = await useLogo(currentSchema);
    },
    { immediate: true }
  );

  useLayoutHead(route);

  const isSignedIn = computed(
    () => !!session.value?.email && session.value?.email !== "anonymous"
  );
  const { menuItems, userMenuItems } = await useLayoutMenu(schema, session);

  return {
    isSignedIn,
    logoUrl,
    menuItems,
    session,
    signOut,
    userMenuItems,
  };
}
