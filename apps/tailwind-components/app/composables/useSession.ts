import { useAsyncData } from "#app/composables/asyncData";
import { computed, type Ref } from "vue";
import { useRoute, useRouter } from "#app/composables/router";
import type { ISession } from "../../types/types";
import { openReAuthenticationWindow } from "../utils/openReAuthenticationWindow";
import { useState } from "#app";

export const useSession = async (schemaId?: string) => {
  const router = useRouter();
  const session = useState("session", () => null as ISession | null);

  let messageHandler: ((event: MessageEvent) => void) | null = null;

  async function fetchSessionDetails() {
    return $fetch("/api/graphql", {
      method: "POST",
      body: JSON.stringify({
        query: `{_session { email, admin, token }}`,
      }),
    });
  }

  async function fetchSchemaRoles(schemaId: string) {
    return $fetch(`/${schemaId}/graphql`, {
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
      if (!session.value.roles) {
        session.value.roles = {};
      }
      session.value.roles[schemaId] =
        schemaRolesResult?.data.value?.data._session.roles;
    }
  }

  async function reload() {
    session.value = null;

    // parallel requests
    const [schemaRolesResult, sessionResult] = await Promise.all([
      schemaId ? fetchSchemaRoles(schemaId) : Promise.resolve(null),
      fetchSessionDetails(),
    ]);

    if (!sessionResult) {
      console.error("Error reloading session");
    }

    session.value = sessionResult.data._session;

    if (session.value && schemaId && schemaRolesResult) {
      if (!session.value.roles) {
        session.value.roles = {};
      }
      session.value.roles[schemaId] = schemaRolesResult.data._session.roles;
    }
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

  function reAuthenticate(
    saveErrorMessage: Ref<string>,
    showReAuthenticateButton: Ref<boolean>,
    formMessage: Ref<string>
  ) {
    const url = router.resolve({
      name: "login",
      query: {
        reauthenticate: "true",
        redirect: encodeURIComponent(window.location.href),
      },
    });
    const reAuthWindow = openReAuthenticationWindow(window, url.href);

    messageHandler = (event) => {
      if (event.origin !== window.location.origin) {
        saveErrorMessage.value = "Error re-authenticating; Invalid origin";
        return;
      }
      if (event.data.status === "reAuthenticated") {
        saveErrorMessage.value = "";
        showReAuthenticateButton.value = false;
        formMessage.value =
          "Re-authenticated, please click 'save' to persist the form changes";
        if (reAuthWindow) {
          reAuthWindow.close();
        }
        if (messageHandler) {
          // remove after handling the message
          window.removeEventListener("message", messageHandler);
        }
      }
    };

    window.addEventListener("message", messageHandler);
    return messageHandler;
  }

  async function signOut() {
    const { data, error } = await $fetch("/api/graphql", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: { query: `mutation { signout { status } }` },
    });

    if (error || data.signout.status !== "SUCCESS") {
      console.error("Error signing out:", error);
      return;
    }

    reload();
  }

  const isAdmin = computed(() => session.value?.admin || false);

  if (
    !session.value ||
    (schemaId && session.value.roles?.[schemaId] === undefined)
  ) {
    await loadSession();
  }

  return {
    isAdmin,
    session,
    reload,
    hasSessionTimeout,
    reAuthenticate,
    signOut,
  };
};
