import { useAsyncData } from "#app/composables/asyncData";
import { computed, ref } from "vue";
import type { ISession } from "../../tailwind-components/types/types";

const session = ref<ISession | null>();

export const useSession = async () => {
  async function loadSession() {
    await useAsyncData("session", async () => {
      const { data, error } = await $fetch("/api/graphql", {
        method: "POST",
        body: JSON.stringify({
          query: `{_session { email, admin, roles, token }}`,
        }),
      });

      if (error) {
        console.error("Error fetching session", error);
        return null;
      }

      session.value = data._session;
    });
  }

  await loadSession();

  function reload() {
    session.value = null;
    loadSession();
  }

  const isAdmin = computed(() => session.value?.admin || false);

  return { isAdmin, session, reload };
};
