import { beforeEach, describe, expect, test, vi } from "vitest";
import { ref } from "vue";
import { flushPromises } from "@vue/test-utils";
import type {
  TaskStatus,
  TruncateStatus,
} from "../../../../metadata-utils/src/types";
import { SessionExpiredError } from "../../../app/utils/sessionExpiredError";

vi.mock("../../../app/composables/useTask", () => ({
  useTask: vi.fn(),
}));

vi.mock("../../../app/composables/useSession", () => ({
  useSession: vi.fn(),
}));

import { useTask } from "../../../app/composables/useTask";
import { useSession } from "../../../app/composables/useSession";
import { useTable } from "../../../app/composables/useTable";

describe("useTable", () => {
  const fetchMock = vi.fn();
  const pollStatusMock = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    vi.stubGlobal("$fetch", fetchMock);

    vi.mocked(useTask).mockReturnValue({
      pollStatus: pollStatusMock,
    } as unknown as ReturnType<typeof useTask>);

    vi.mocked(useSession).mockResolvedValue({
      hasSessionTimeout: vi.fn().mockResolvedValue(false),
    } as unknown as Awaited<ReturnType<typeof useSession>>);
  });

  test("deleteRecords calls graphql delete mutation with keys", async () => {
    fetchMock.mockResolvedValueOnce({ data: { delete: { message: "ok" } } });

    const { deleteRecords } = useTable("demo", "Person");
    const keys = new Set([{ id: "1" }, { id: "2" }]);

    await deleteRecords(keys);

    expect(fetchMock).toHaveBeenCalledWith("/demo/graphql", {
      method: "POST",
      body: {
        query:
          "mutation delete($pkey:[PersonInput]){delete(Person:$pkey){message}}",
        variables: { pkey: [{ id: "1" }, { id: "2" }] },
      },
    });
  });

  test("deleteRecords throws SessionExpiredError on timed out session", async () => {
    const fetchError = { statusCode: 401 };
    fetchMock.mockRejectedValueOnce(fetchError);

    vi.mocked(useSession).mockResolvedValue({
      hasSessionTimeout: vi.fn().mockResolvedValue(true),
    } as unknown as Awaited<ReturnType<typeof useSession>>);

    const { deleteRecords } = useTable("demo", "Person");

    await expect(deleteRecords(new Set([{ id: "1" }]))).rejects.toBeInstanceOf(
      SessionExpiredError
    );
  });

  test("truncate sets status to COMPLETED when task poll succeeds", async () => {
    fetchMock.mockResolvedValueOnce({
      data: {
        truncate: {
          taskId: "task-1",
          message: "queued",
        },
      },
    });
    pollStatusMock.mockResolvedValueOnce({
      status: "COMPLETED" satisfies TaskStatus,
      description: "done",
    });

    const { truncate } = useTable("demo", "Person");
    const truncateStatus = ref<TruncateStatus>("IDLE");

    await expect(truncate(truncateStatus)).resolves.toEqual({
      status: "COMPLETED",
    });
    await flushPromises();

    expect(vi.mocked(useTask)).toHaveBeenCalledWith("demo", "task-1");
    expect(pollStatusMock).toHaveBeenCalledTimes(1);
    expect(truncateStatus.value).toBe("COMPLETED");
  });

  test("truncate returns FAILED with description when task poll returns ERROR", async () => {
    fetchMock.mockResolvedValueOnce({
      data: {
        truncate: {
          taskId: "task-2",
          message: "queued",
        },
      },
    });
    pollStatusMock.mockResolvedValueOnce({
      status: "ERROR" satisfies TaskStatus,
      description: "truncate failed",
    });

    const { truncate } = useTable("demo", "Person");
    const truncateStatus = ref<TruncateStatus>("IDLE");

    await expect(truncate(truncateStatus)).resolves.toEqual({
      status: "FAILED",
      description: "truncate failed",
    });
    await flushPromises();

    expect(truncateStatus.value).toBe("FAILED");
  });

  test("truncate returns FAILED when task polling rejects", async () => {
    fetchMock.mockResolvedValueOnce({
      data: {
        truncate: {
          taskId: "task-3",
          message: "queued",
        },
      },
    });
    pollStatusMock.mockRejectedValueOnce(new Error("poll failed"));

    const { truncate } = useTable("demo", "Person");
    const truncateStatus = ref<TruncateStatus>("IDLE");

    await expect(truncate(truncateStatus)).resolves.toEqual({
      status: "FAILED",
      description: "poll failed",
    });

    expect(truncateStatus.value).toBe("FAILED");
  });

  test("truncate returns FAILED when no task id is returned", async () => {
    fetchMock.mockResolvedValueOnce({
      data: {
        truncate: {
          taskId: "",
          message: "queued",
        },
      },
    });

    const { truncate } = useTable("demo", "Person");
    const truncateStatus = ref<TruncateStatus>("IDLE");

    await expect(truncate(truncateStatus)).resolves.toEqual({
      status: "FAILED",
      description: "No task returned from truncate operation",
    });

    expect(truncateStatus.value).toBe("FAILED");
  });
});
