<template>
  <div>
    <div v-if="loading && !workers.length" class="text-center py-4">Loading...</div>
    <div v-else-if="error" class="alert alert-danger">{{ error }}</div>
    <div v-else-if="!workers.length" class="text-center text-muted py-4">
      No workers registered
    </div>
    <div v-else>
      <table class="table table-sm table-hover">
        <thead>
          <tr>
            <th>Worker ID</th>
            <th>Hostname</th>
            <th>Capabilities</th>
            <th>Registered</th>
            <th>Last Heartbeat</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="w in workers" :key="w.worker_id">
            <td><code>{{ w.worker_id }}</code></td>
            <td>{{ w.hostname || "-" }}</td>
            <td>
              <span
                v-for="(cap, i) in w.capabilities"
                :key="i"
                class="badge bg-light text-dark border me-1"
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
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from "vue";
import { fetchWorkers } from "../composables/useHpcApi.js";
import { formatDate } from "../utils/jobs.js";

const workers = ref([]);
const loading = ref(false);
const error = ref(null);
let refreshInterval = null;

function heartbeatClass(ts) {
  if (!ts) return "text-muted";
  const age = Date.now() - new Date(ts).getTime();
  // stale if no heartbeat in 5 minutes
  return age > 5 * 60 * 1000 ? "text-danger" : "text-success";
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
