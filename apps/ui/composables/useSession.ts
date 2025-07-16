import { useAsyncData } from "#app/composables/asyncData";
import { computed, ref } from "vue";
import type { ISession } from "../../tailwind-components/types/types";

const session = ref<ISession | null>();
const sessionLoaded = ref(false);

export const useSession = () => {
  function loadSession() {
    useAsyncData("session", async () => {
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
      sessionLoaded.value = true;
    });
  }

  loadSession();

  function reload() {
    session.value = null;
    sessionLoaded.value = false;
    loadSession();
  }

  const isAdmin = computed(() => session.value?.email === "admin");

  return { isAdmin, session, sessionLoaded, reload };
};
