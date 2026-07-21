import { SessionExpiredError } from "../utils/sessionExpiredError";
import type { columnValue } from "../../../metadata-utils/src/types";
import { useSession } from "./useSession";

export const useTable = () => {
  const deleteRecords = async (
    schemaId: string,
    tableId: string,
    keys: Set<Record<string, columnValue>>
  ) => {
    const query = `mutation delete($pkey:[${tableId}Input]){delete(${tableId}:$pkey){message}}`;
    const variables = { pkey: Array.from(keys) };

    return $fetch(`/${schemaId}/graphql`, {
      method: "POST",
      body: {
        query,
        variables,
      },
    }).catch((error) =>
      handleFetchError(error, "Error on delete records from table " + tableId)
    );
  };

  async function handleFetchError(error: any, message: string) {
    if (error.statusCode && error.statusCode >= 400) {
      const { hasSessionTimeout } = await useSession();
      if (await hasSessionTimeout()) {
        console.log("Session has timed out, ask for re-authentication");
        throw new SessionExpiredError(
          "Session has expired, please log in again."
        );
      }
    }
    // if we don't suspect a session timeout, rethrow the original error
    throw error;
  }

  return {
    deleteRecords,
  };
};
