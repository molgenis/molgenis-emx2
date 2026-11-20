import { useAsyncData, useRequestFetch } from "#app";
import { computed } from "vue";
import { fetchSettings } from "../../../tailwind-components/app/utils/fetchSettings";
import type { ISetting } from "../../../metadata-utils/src/types";
import {
  PAPER_EXTRACTING_SETTING_KEYS,
  parsePaperExtractingSettings,
} from "../utils/paperExtractingSettings";

const SUBMIT_ROLE_PREFIX = "Submit_";
const OWNER_ROLE = "Owner";
const ANONYMOUS_USER = "anonymous";

interface SessionResponse {
  data?: {
    _session?: { email?: string; admin?: boolean; roles?: string[] };
  };
}

export interface PaperExtractingAccess {
  isSignedIn: boolean;
  hasAccess: boolean;
}

const NO_ACCESS: PaperExtractingAccess = {
  isSignedIn: false,
  hasAccess: false,
};

/**
 * Loads the CATALOGUE_ENABLE_PAPER_EXTRACTING and
 * CATALOGUE_PAPER_EXTRACTING_SCHEMA settings of the catalogue schema.
 */
export async function usePaperExtractingSettings() {
  const { data } = await useAsyncData(
    "paper-extracting-settings",
    () =>
      fetchSettings(PAPER_EXTRACTING_SETTING_KEYS) as Promise<{
        data?: { _settings?: ISetting[] };
      }>
  );
  return parsePaperExtractingSettings(data.value?.data?._settings);
}

/**
 * Returns a function that checks if the current user may use paper extracting
 * in the given schema: the user must be signed in as admin,
 * or have the Owner role or a custom role starting with 'Submit_' on that schema.
 *
 * Must be called synchronously in a Nuxt context (setup, middleware, plugin),
 * the returned function can then safely be awaited.
 */
export function usePaperExtractingAccessCheck(
  schema: string
): () => Promise<PaperExtractingAccess> {
  // forwards the request cookies during SSR so the user's session is used
  const requestFetch = useRequestFetch();

  return async () => {
    if (!schema) {
      return NO_ACCESS;
    }
    try {
      const response = await requestFetch<SessionResponse>(
        `/${schema}/graphql`,
        {
          method: "POST",
          body: { query: "{_session { email admin roles }}" },
        }
      );
      const session = response?.data?._session;
      if (!session?.email || session.email === ANONYMOUS_USER) {
        return NO_ACCESS;
      }
      const hasAccess =
        !!session.admin ||
        (session.roles ?? []).some(
          (role) => role === OWNER_ROLE || role.startsWith(SUBMIT_ROLE_PREFIX)
        );
      return { isSignedIn: true, hasAccess };
    } catch (error) {
      console.error("Error checking paper extracting access:", error);
      return NO_ACCESS;
    }
  };
}

export async function usePaperExtractingAccess(schema: string) {
  const checkAccess = usePaperExtractingAccessCheck(schema);
  const { data } = await useAsyncData("paper-extracting-access", checkAccess);
  return {
    isSignedIn: computed(() => data.value?.isSignedIn === true),
    hasAccess: computed(() => data.value?.hasAccess === true),
  };
}
