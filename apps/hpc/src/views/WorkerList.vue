<template>
  <div class="hpc-page-view">
    <section class="hpc-surface hpc-toolbar-card">
      <div class="hpc-toolbar-row">
        <div>
          <p class="hpc-toolbar-title">Workers</p>
          <p class="hpc-toolbar-subtitle">
            Track worker heartbeats and capability profiles. Data refreshes automatically every 15s.
          </p>
        </div>
        <div class="hpc-toolbar-controls">
          <span class="hpc-meta-chip">
            <strong>Registered</strong>
            {{ workers.length }}
          </span>
        </div>
      </div>
    </section>

    <div v-if="loading && !workers.length" class="hpc-surface hpc-feedback text-center">
      Loading workers...
    </div>
    <div v-else-if="error" class="alert alert-danger hpc-feedback mb-0">{{ error }}</div>
    <div v-else-if="!workers.length" class="hpc-surface hpc-empty text-center">
      <p class="text-muted">No workers registered</p>
    </div>
    <div v-else>
      <section class="hpc-surface hpc-table-card">
        <div class="hpc-table-wrap">
          <table class="table table-sm table-hover">
            <thead>
              <tr>
                <th>Worker ID</th>
                <th>Hostname</th>
                <th>Capabilities</th>
                <th>Registered</th>
                <th>Last Heartbeat</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="w in workers" :key="w.worker_id">
                <td><code class="hpc-inline-code">{{ w.worker_id }}</code></td>
                <td>{{ w.hostname || "-" }}</td>
                <td>
                  <span
                    v-for="(cap, i) in w.capabilities"
                    :key="i"
                    class="badge me-1 mb-1 hpc-badge-chip"
                  >
                    {{ cap.processor }}:{{ cap.profile }}
                  </span>
                  <span v-if="!w.capabilities.length" class="text-muted">-</span>
                </td>
                <td>{{ formatDate(w.registered_at) }}</td>
                <td>
                  <span :class="heartbeatClass(w.last_heartbeat_at)">
                    {{ formatDate(w.last_heartbeat_at) }}
                  </span>
                </td>
                <td>
                  <button
                    class="btn btn-outline-danger btn-sm"
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

<script setup>
import { ref, onMounted, onUnmounted } from "vue";
import { fetchWorkers, deleteWorker } from "../composables/useHpcApi.js";
import { formatDate } from "../utils/jobs.js";

const workers = ref([]);
const loading = ref(false);
const error = ref(null);
const deletingWorker = ref(null);
let refreshInterval = null;

function heartbeatClass(ts) {
  if (!ts) return "text-muted";
  const age = Date.now() - new Date(ts).getTime();
  // stale if no heartbeat in 5 minutes
  return age > 5 * 60 * 1000 ? "text-danger" : "text-success";
}

async function onDeleteWorker(workerId) {
  if (!confirm(`Remove worker "${workerId}"? Jobs assigned to this worker will retain their history.`)) return;
  deletingWorker.value = workerId;
  try {
    await deleteWorker(workerId);
    await loadWorkers();
  } catch (e) {
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
  } catch (e) {
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
  clearInterval(refreshInterval);
});
</script>
