import { beforeEach, afterEach, describe, expect, test, vi } from "vitest";
import { ref } from "vue";
import type { TaskStatus } from "../../../../metadata-utils/src/types";
import { useTask } from "../../../app/composables/useTask";

describe("useTask", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    vi.stubGlobal("$fetch", fetchMock);
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.unstubAllGlobals();
  });

  test("resolves immediately when API returns completed", async () => {
    fetchMock.mockResolvedValueOnce({
      data: { _tasks: [{ status: "COMPLETED", description: "done" }] },
    });

    const status = ref<TaskStatus>("RUNNING");
    const { pollStatus } = useTask("demo", "task-1");

    await expect(pollStatus(status)).resolves.toEqual({
      status: "COMPLETED",
      description: "done",
    });
    expect(status.value).toBe("COMPLETED");
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(fetchMock).toHaveBeenCalledWith("/demo/graphql", {
      method: "POST",
      body: {
        query:
          'query { _tasks(id:"task-1") { id, description, status, subTasks { id, description, status} } }',
      },
    });
  });

  test("keeps polling until API returns completed", async () => {
    vi.useFakeTimers();
    fetchMock
      .mockResolvedValueOnce({ data: { _tasks: [{ status: "RUNNING" }] } })
      .mockResolvedValueOnce({
        data: { _tasks: [{ status: "COMPLETED", description: "done" }] },
      });

    const status = ref<TaskStatus>("RUNNING");
    const { pollStatus } = useTask("demo", "task-2");

    const resultPromise = pollStatus(status, 1000);
    await Promise.resolve();

    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(status.value).toBe("RUNNING");

    await vi.advanceTimersByTimeAsync(1000);

    await expect(resultPromise).resolves.toEqual({
      status: "COMPLETED",
      description: "done",
    });
    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(status.value).toBe("COMPLETED");
  });

  test("rejects and sets status to UNKNOWN when response has no task status", async () => {
    fetchMock.mockResolvedValueOnce({
      data: { _tasks: [] },
    });

    const status = ref<TaskStatus>("RUNNING");
    const { pollStatus } = useTask("demo", "task-3");

    await expect(pollStatus(status)).rejects.toThrow("Task status not found");
    expect(status.value).toBe("UNKNOWN");
  });

  test("rejects when API returns terminal error status", async () => {
    fetchMock.mockResolvedValueOnce({
      data: { _tasks: [{ status: "ERROR" }] },
    });

    const status = ref<TaskStatus>("WAITING");
    const { pollStatus } = useTask("demo", "task-5");

    await expect(pollStatus(status)).resolves.toEqual({
      status: "ERROR",
      description: undefined,
    });
    expect(status.value).toBe("ERROR");
  });

  test("rejects when fetch fails", async () => {
    fetchMock.mockRejectedValueOnce(new Error("network error"));

    const status = ref<TaskStatus>("RUNNING");
    const { pollStatus } = useTask("demo", "task-4");

    await expect(pollStatus(status)).rejects.toThrow("network error");
  });
});
