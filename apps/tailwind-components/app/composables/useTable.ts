import { SessionExpiredError } from "../utils/sessionExpiredError";
import type {
  columnValue,
  IColumn,
  ITableMetaData,
  TaskStatus,
  TruncateStatus,
} from "../../../metadata-utils/src/types";
import { useSession } from "./useSession";
import { ref, type Ref } from "vue";
import { useTask } from "./useTask";
import fetchMetadata from "./fetchMetadata";

interface TruncateResponse {
  data: {
    truncate: {
      taskId: string;
      message: string;
    };
  };
}

export const useTable = (schemaId: string, tableId: string) => {
  const deleteRecords = async (keys: Set<Record<string, columnValue>>) => {
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

  const truncate = async (truncateStatus: Ref<TruncateStatus>) => {
    truncateStatus.value = "RUNNING";
    try {
      const truncateResp = await $fetch<TruncateResponse>(
        `/${schemaId}/graphql`,
        {
          method: "POST",
          body: {
            query: `mutation { truncate(tables: "${tableId}", async: true) { taskId, message } }`,
          },
        }
      );

      const truncateTaskId = truncateResp.data.truncate.taskId;
      if (!truncateTaskId) {
        throw new Error("No task returned from truncate operation");
      }

      const taskStatus = ref<TaskStatus>("WAITING");

      const finalStatus = await useTask(schemaId, truncateTaskId)
        .pollStatus(taskStatus)
        .catch((error) => {
          console.error("Error polling task status:", error);
          throw error; // Rethrow the error to be caught by the outer try-catch
        });

      if (finalStatus.status === "COMPLETED") {
        console.log("Truncate operation completed successfully.");
        truncateStatus.value = "COMPLETED";
        return { status: truncateStatus.value };
      } else {
        console.error("Truncate operation failed.");
        truncateStatus.value = "FAILED";
        return {
          status: truncateStatus.value,
          description: finalStatus.description,
        };
      }
    } catch (error) {
      console.error("Error truncating table:", error);
      truncateStatus.value = "FAILED";
      return {
        status: truncateStatus.value,
        description: (error as Error).message,
      };
    }
  };

  const cascadeDeleteConfirmationMsg = async () => {
    const schema = await fetchMetadata(schemaId);
    function findReferingTables(
      tableId: string,
      found: Record<string, ITableMetaData>
    ) {
      const referringTables = schema.tables
        .filter((table) => table.id !== tableId && !found[table.id])
        .filter((table: ITableMetaData) => {
          return table.columns.find(
            (column: IColumn) =>
              column.refTableId === tableId &&
              (column.columnType === "REF" ||
                column.columnType === "SELECT" ||
                column.columnType === "RADIO") &&
              column.cascadeDelete
          );
        });
      referringTables.forEach((table) => {
        found[table.id] = table;
        findReferingTables(table.id, found);
      });
    }
    const found: Record<string, ITableMetaData> = {};
    // recursively find tables that reference this table, passing the found tables so we don't get into an infinite loop
    findReferingTables(tableId, found);
    const cascadeTables = Object.values(found);

    return cascadeTables.length
      ? "Removing this row will also remove any rows in the following tables that reference this row: " +
          cascadeTables.map((table) => table.name).join(", ")
      : "";
  };

  async function handleFetchError(error: any, message: string) {
    if (error.statusCode && error.statusCode >= 400) {
      const { hasSessionTimeout } = useSession();
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
    cascadeDeleteConfirmationMsg,
    deleteRecords,
    truncate,
  };
};
