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
                <th class="px-4 py-3 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider">
                  Worker ID
                </th>
                <th class="px-4 py-3 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider">
                  Hostname
                </th>
                <th class="px-4 py-3 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider">
                  Capabilities
                </th>
                <th class="px-4 py-3 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider">
                  Registered
                </th>
                <th class="px-4 py-3 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider">
                  Last Heartbeat
                </th>
                <th class="px-4 py-3 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider"></th>
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
                  <span v-if="!w.capabilities.length" class="text-definition-list-term"
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
import { formatDate } from "../utils/jobs";

const workers = ref<any[]>([]);
const loading = ref(false);
const error = ref<string | null>(null);
const deletingWorker = ref<string | null>(null);
let refreshInterval: ReturnType<typeof setInterval> | null = null;

function heartbeatClass(ts: string | null): string {
  if (!ts) return "text-definition-list-term";
  const age = Date.now() - new Date(ts).getTime();
  return age > 5 * 60 * 1000 ? "text-red-700" : "text-green-500";
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

async function loadWorkers() {
  loading.value = true;
  error.value = null;
  try {
    workers.value = await fetchWorkers();
  } catch (e: any) {
    error.value = e.message;
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadWorkers();
  refreshInterval = setInterval(loadWorkers, 15000);
});

onUnmounted(() => {
  if (refreshInterval) clearInterval(refreshInterval);
});
</script>
