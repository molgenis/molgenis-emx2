import { useAsyncData } from "#app/composables/asyncData";
import { computed, ref } from "vue";
import type { ISession } from "../types/types";

const session = ref<ISession | null>();

export const useSession = async () => {
  async function fetchSessionDetails() {
    return await $fetch("/api/graphql", {
      method: "POST",
      body: JSON.stringify({
        query: `{_session { email, admin, roles, token }}`,
      }),
    });
  }

  async function loadSession() {
    await useAsyncData("session", async () => {
      const { data, error } = await fetchSessionDetails();
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

  const hasSessionTimeout = async () => {
    const { data, error } = await fetchSessionDetails();
    if (error) {
      console.error("Error testing session timeout", error);
      return false;
    }

    if (session.value?.email !== data._session.email) {
      console.warn("Session has expired");
      return true;
    }

    return false;
  };

  return { isAdmin, session, reload, hasSessionTimeout };
};
