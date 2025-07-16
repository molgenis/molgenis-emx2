import { useAsyncData } from "#app/composables/asyncData";
import { computed, ref } from "vue";
import type { ISession } from "../../tailwind-components/types/types";

const session = ref<ISession | null>();
const sessionPromise = ref<Promise<{ data: any; error: any }> | null>(null);

export const useSession = () => {
  function loadSession() {
    useAsyncData("session", async () => {
      sessionPromise.value = $fetch("/api/graphql", {
        method: "POST",
        body: JSON.stringify({
          query: `{_session { email, admin, roles, token }}`,
        }),
      });
      const { data, error } = await sessionPromise.value;

      if (error) {
        console.error("Error fetching session", error);
        return null;
      }

      session.value = data._session;
    });
  }

  loadSession();

  function reload() {
    session.value = null;
    loadSession();
  }

  const isAdmin = computed(() => session.value?.email === "admin");

  return { isAdmin, session, sessionPromise, reload };
};
