import { useAsyncData } from "#app/composables/asyncData";
import { computed, ref } from "vue";
import { useRoute } from "#app/composables/router";
import type { ISession } from "../types/types";

const session = ref<ISession | null>();

export const useSession = async () => {
  const route = useRoute();
  const schemaId = route.params.schema as string | null;

  async function fetchSessionDetails() {
    return await $fetch("/api/graphql", {
      method: "POST",
      body: JSON.stringify({
        query: `{_session { email, admin, token }}`,
      }),
    });
  }

  async function fetchSchemaRoles(schemaId: string) {
    return await $fetch(`/${schemaId}/graphql`, {
      method: "POST",
      body: JSON.stringify({
        query: `{_session { roles }}`,
      }),
    });
  }

  async function loadSession() {
    const schemaRolesPromise = schemaId
      ? useAsyncData("schemaRoles_" + schemaId, () =>
          fetchSchemaRoles(schemaId)
        )
      : Promise.resolve(null);

    const sessionPromise = useAsyncData("session", () => fetchSessionDetails());

    // parallel requests
    const [schemaRolesResult, sessionResult] = await Promise.all([
      schemaRolesPromise,
      sessionPromise,
    ]);

    if (sessionResult.error.value || schemaRolesResult?.error.value) {
      console.error("Error fetching session", sessionResult.error.value);
    }

    session.value = sessionResult.data.value?.data._session;

    if (session.value && schemaId) {
      session.value.roles = schemaRolesResult?.data.value?.data._session.roles;
    }
  }

  async function reload() {
    session.value = null;
    await loadSession();
  }

  async function hasSessionTimeout() {
    const { data, error } = await fetchSessionDetails();
    if (error) {
      console.error("Error testing session timeout", error);
      return false;
    }

    // compare the current frontend session user to the current backend session user
    return session.value?.email !== data._session.email;
  }

  const isAdmin = computed(() => session.value?.admin || false);

  if (!session.value) {
    await loadSession();
  }

  return { isAdmin, session, reload, hasSessionTimeout };
};
