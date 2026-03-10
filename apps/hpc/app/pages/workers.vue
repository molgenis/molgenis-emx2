<template>
  <div class="space-y-4">
    <section class="bg-form rounded-lg border border-color-theme p-6">
      <div class="flex items-start justify-between">
        <div>
          <p class="text-lg font-semibold text-title">Workers</p>
          <p class="text-sm text-definition-list-term">
            Track worker heartbeats and capability profiles. Data refreshes
            automatically every 15s.
          </p>
        </div>
        <span
          class="inline-flex items-center gap-1 px-3 py-1 rounded-full bg-content text-sm text-title"
        >
          <strong>Registered</strong>
          {{ workers.length }}
        </span>
        <span class="text-xs text-definition-list-term">
          {{ refreshing ? "Refreshing..." : "Auto-refresh: 15s" }}
        </span>
      </div>
    </section>

    <div
      v-if="loading && !workers.length"
      class="bg-form rounded-lg border border-color-theme p-6 text-center text-definition-list-term"
    >
      Loading workers...
    </div>
    <div
      v-else-if="error"
      class="bg-red-500/10 border border-red-500/20 text-red-700 p-4 rounded-lg"
    >
      {{ error }}
    </div>
    <div
      v-else-if="!workers.length"
      class="bg-form rounded-lg border border-color-theme p-6 text-center"
    >
      <p class="text-definition-list-term">No workers registered</p>
    </div>
    <div v-else>
      <section class="bg-form rounded-lg border border-color-theme">
        <div class="overflow-x-auto">
          <table class="w-full text-sm text-table-row">
            <thead>
              <tr class="border-b border-color-theme">
                <th
                  class="px-4 py-3 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider"
                >
                  Worker ID
                </th>
                <th
                  class="px-4 py-3 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider"
                >
                  Hostname
                </th>
                <th
                  class="px-4 py-3 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider"
                >
                  Capabilities
                </th>
                <th
                  class="px-4 py-3 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider"
                >
                  Registered
                </th>
                <th
                  class="px-4 py-3 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider"
                >
                  Last Heartbeat
                </th>
                <th
                  class="px-4 py-3 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider"
                >
                  Active Jobs
                </th>
                <th
                  class="px-4 py-3 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider"
                ></th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="w in workers"
                :key="w.worker_id"
                class="border-b border-color-theme hover:bg-hover transition-colors"
              >
                <td class="px-4 py-3">
                  <code class="text-xs bg-content px-1.5 py-0.5 rounded">{{
                    w.worker_id
                  }}</code>
                </td>
                <td class="px-4 py-3">{{ w.hostname || "-" }}</td>
                <td class="px-4 py-3">
                  <span
                    v-for="(cap, i) in w.capabilities"
                    :key="i"
                    class="inline-flex items-center mr-1 mb-1 px-2 py-0.5 rounded-full text-xs font-medium bg-content text-record-label"
                  >
                    {{ cap.processor }}:{{ cap.profile }}
                  </span>
                  <span
                    v-if="!w.capabilities.length"
                    class="text-definition-list-term"
                    >-</span
                  >
                </td>
                <td class="px-4 py-3">
                  {{ formatDate(w.registered_at) }}
                </td>
                <td class="px-4 py-3">
                  <span :class="heartbeatClass(w.last_heartbeat_at)">
                    {{ formatDate(w.last_heartbeat_at) }}
                  </span>
                </td>
                <td class="px-4 py-3 min-w-[260px]">
                  <div v-if="w.active_jobs?.length" class="space-y-2">
                    <div
                      v-for="activeJob in w.active_jobs"
                      :key="activeJob.id"
                      class="rounded border border-color-theme p-2 bg-content"
                    >
                      <div class="flex items-center gap-2 mb-1">
                        <NuxtLink
                          :to="`/jobs/${activeJob.id}`"
                          class="text-xs font-mono text-button-outline hover:text-button-outline-hover underline underline-offset-2"
                        >
                          {{ shortId(activeJob.id) }}
                        </NuxtLink>
                        <StatusBadge :status="activeJob.status" />
                      </div>
                      <div v-if="hasJobProgress(activeJob)" class="space-y-1">
                        <div
                          class="flex items-center justify-between text-xs text-definition-list-term"
                        >
                          <span class="truncate">
                            {{
                              activeJob.phase ||
                              activeJob.message ||
                              "In progress"
                            }}
                          </span>
                          <span>{{
                            formatProgressPercent(activeJob.progress)
                          }}</span>
                        </div>
                        <div class="h-1.5 bg-form rounded overflow-hidden">
                          <div
                            class="h-full bg-blue-500 transition-all duration-300"
                            :style="{
                              width: `${Math.max(
                                0,
                                Math.min(
                                  100,
                                  Math.round((activeJob.progress ?? 0) * 100)
                                )
                              )}%`,
                            }"
                          />
                        </div>
                      </div>
                    </div>
                  </div>
                  <span v-else class="text-definition-list-term">-</span>
                </td>
                <td class="px-4 py-3">
                  <button
                    class="px-2 py-1 text-xs border border-red-500 text-red-700 rounded hover:bg-red-500/10 disabled:opacity-50"
                    title="Remove worker"
                    :disabled="deletingWorker === w.worker_id"
                    @click.stop="onDeleteWorker(w.worker_id)"
                  >
                    Remove
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from "vue";
import { fetchWorkers, deleteWorker } from "../composables/useHpcApi";
import { formatDate, formatProgressPercent } from "../utils/jobs";

const workers = ref<any[]>([]);
const loading = ref(false);
const refreshing = ref(false);
const error = ref<string | null>(null);
const deletingWorker = ref<string | null>(null);
let refreshInterval: ReturnType<typeof setInterval> | null = null;
let initialLoadDone = false;

function heartbeatClass(ts: string | null): string {
  if (!ts) return "text-definition-list-term";
  const age = Date.now() - new Date(ts).getTime();
  return age > 5 * 60 * 1000 ? "text-red-700" : "text-green-500";
}

function shortId(idVal: string): string {
  return idVal?.substring?.(0, 8) || idVal || "-";
}

function hasJobProgress(job: any): boolean {
  return (
    typeof job?.progress === "number" ||
    typeof job?.phase === "string" ||
    typeof job?.message === "string"
  );
}

function mergeWorkers(nextWorkers: any[]) {
  const previousById = new Map(
    workers.value.map((worker: any) => [worker.worker_id, worker])
  );
  workers.value = nextWorkers.map((nextWorker: any) => {
    const previous = previousById.get(nextWorker.worker_id);
    if (!previous) return nextWorker;
    Object.assign(previous, nextWorker);
    return previous;
  });
}

async function onDeleteWorker(workerId: string) {
  if (
    !confirm(
      `Remove worker "${workerId}"? Jobs assigned to this worker will retain their history.`
    )
  )
    return;
  deletingWorker.value = workerId;
  try {
    await deleteWorker(workerId);
    await loadWorkers();
  } catch (e: any) {
    error.value = e.message;
  } finally {
    deletingWorker.value = null;
  }
}

async function loadWorkers({
  background = false,
}: { background?: boolean } = {}) {
  if (!initialLoadDone && !background) loading.value = true;
  if (background) refreshing.value = true;
  if (!background) error.value = null;
  try {
    const fetched = await fetchWorkers();
    mergeWorkers(fetched);
  } catch (e: any) {
    error.value = e.message;
  } finally {
    if (!initialLoadDone) {
      loading.value = false;
      initialLoadDone = true;
    }
    if (background) refreshing.value = false;
  }
}

onMounted(() => {
  loadWorkers();
  refreshInterval = setInterval(() => loadWorkers({ background: true }), 15000);
});

onUnmounted(() => {
  if (refreshInterval) clearInterval(refreshInterval);
});
</script>
