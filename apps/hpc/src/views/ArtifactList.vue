<template>
  <div>
    <div class="d-flex justify-content-between align-items-center mb-3">
      <div class="d-flex gap-2">
        <select v-model="statusFilter" class="form-select form-select-sm" style="width: auto">
          <option value="">All Statuses</option>
          <option v-for="s in statuses" :key="s" :value="s">{{ s }}</option>
        </select>
      </div>
      <button class="btn btn-primary btn-sm" @click="showForm = !showForm">
        {{ showForm ? "Hide Form" : "+ New Artifact" }}
      </button>
    </div>

    <ArtifactUploadForm
      v-if="showForm"
      @created="onArtifactCreated"
      @close="showForm = false"
    />

    <div v-if="loading && !items.length" class="text-center py-4">Loading...</div>
    <div v-else-if="error" class="alert alert-danger">{{ error }}</div>
    <div v-else>
      <table class="table table-sm table-hover">
        <thead>
          <tr>
            <th>ID</th>
            <th>Name</th>
            <th>Type</th>
            <th>Format</th>
            <th>Residence</th>
            <th>Status</th>
            <th>Size</th>
            <th>Created</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="a in items"
            :key="a.id"
            style="cursor: pointer"
            @click="$router.push(`/artifacts/${a.id}`)"
          >
            <td><code>{{ a.id?.substring(0, 8) }}</code></td>
            <td>{{ a.name || a.id?.substring(0, 8) }}</td>
            <td>{{ a.type || "-" }}</td>
            <td>{{ a.format || "-" }}</td>
            <td>{{ a.residence || "-" }}</td>
            <td><StatusBadge :status="a.status" /></td>
            <td>{{ formatSize(a.size_bytes) }}</td>
            <td>{{ formatDate(a.created_at) }}</td>
          </tr>
          <tr v-if="!items.length">
            <td colspan="8" class="text-center text-muted">No artifacts found</td>
          </tr>
        </tbody>
      </table>

      <div class="d-flex justify-content-between align-items-center">
        <small class="text-muted">
          Showing {{ items.length }} of {{ totalCount }} artifacts
        </small>
        <div class="btn-group btn-group-sm">
          <button
            class="btn btn-outline-secondary"
            :disabled="offset === 0"
            @click="offset = Math.max(0, offset - limit)"
          >
            &laquo; Prev
          </button>
          <button
            class="btn btn-outline-secondary"
            :disabled="offset + limit >= totalCount"
            @click="offset += limit"
          >
            Next &raquo;
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted } from "vue";
import { fetchArtifacts } from "../composables/useHpcApi.js";
import { formatDate } from "../utils/jobs.js";
import StatusBadge from "../components/StatusBadge.vue";
import ArtifactUploadForm from "../components/ArtifactUploadForm.vue";

const statuses = ["CREATED", "UPLOADING", "REGISTERED", "COMMITTED", "FAILED"];

const items = ref([]);
const totalCount = ref(0);
const loading = ref(false);
const error = ref(null);
const statusFilter = ref("");
const offset = ref(0);
const limit = ref(25);
const showForm = ref(false);

let refreshInterval = null;

function formatSize(bytes) {
  if (bytes == null) return "-";
  const n = Number(bytes);
  if (n < 1024) return `${n} B`;
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`;
  return `${(n / (1024 * 1024)).toFixed(1)} MB`;
}

async function loadArtifacts() {
  loading.value = true;
  error.value = null;
  try {
    const result = await fetchArtifacts({
      status: statusFilter.value || undefined,
      limit: limit.value,
      offset: offset.value,
    });
    items.value = result.items;
    totalCount.value = result.totalCount;
  } catch (e) {
    error.value = e.message;
  } finally {
    loading.value = false;
  }
}

function onArtifactCreated() {
  showForm.value = false;
  loadArtifacts();
}

watch([statusFilter], () => {
  offset.value = 0;
  loadArtifacts();
});

watch(offset, loadArtifacts);

onMounted(() => {
  loadArtifacts();
  refreshInterval = setInterval(loadArtifacts, 10000);
});

onUnmounted(() => {
  clearInterval(refreshInterval);
});
</script>
