<template>
  <div class="hpc-page-view">
    <section class="hpc-surface hpc-toolbar-card">
      <div class="hpc-toolbar-row">
        <div>
          <p class="hpc-toolbar-title">Artifacts</p>
          <p class="hpc-toolbar-subtitle">
            Browse uploaded outputs and inspect artifact metadata and file contents.
          </p>
        </div>
        <div class="hpc-toolbar-controls">
          <select v-model="statusFilter" class="form-select form-select-sm">
            <option value="">All Statuses</option>
            <option v-for="s in statuses" :key="s" :value="s">{{ s }}</option>
          </select>
          <button class="btn btn-primary btn-sm" @click="showForm = !showForm">
            {{ showForm ? "Hide Form" : "+ New Artifact" }}
          </button>
        </div>
      </div>
    </section>

    <ArtifactUploadForm
      v-if="showForm"
      class="hpc-surface"
      @created="onArtifactCreated"
      @close="showForm = false"
    />

    <div v-if="loading && !items.length" class="hpc-surface hpc-feedback text-center">
      Loading artifacts...
    </div>
    <div v-else-if="error" class="alert alert-danger hpc-feedback mb-0">{{ error }}</div>
    <div v-else>
      <section class="hpc-surface hpc-table-card">
        <div class="hpc-table-wrap">
          <table class="table table-sm table-hover">
            <thead>
              <tr>
                <th>ID</th>
                <th>Name</th>
                <th>Type</th>
                <th>Residence</th>
                <th>Status</th>
                <th>Size</th>
                <th>Created</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="a in items"
                :key="a.id"
                class="hpc-row-link"
                @click="$router.push(`/artifacts/${a.id}`)"
              >
                <td><code class="hpc-inline-code">{{ a.id?.substring(0, 8) }}</code></td>
                <td>{{ a.name || a.id?.substring(0, 8) }}</td>
                <td>{{ a.type || "-" }}</td>
                <td>{{ a.residence || "-" }}</td>
                <td><StatusBadge :status="a.status" /></td>
                <td>{{ formatSize(a.size_bytes) }}</td>
                <td>{{ formatDate(a.created_at) }}</td>
                <td>
                  <button
                    class="btn btn-outline-danger btn-sm hpc-icon-btn"
                    title="Delete artifact"
                    :disabled="deleting"
                    @click.stop="onDelete(a)"
                  >
                    <HpcIconTrash />
                  </button>
                </td>
              </tr>
              <tr v-if="!items.length">
                <td colspan="8" class="text-center text-muted py-4">No artifacts found</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="hpc-table-footer">
          <small class="hpc-muted-note">
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
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted } from "vue";
import { fetchArtifacts, deleteArtifact } from "../composables/useHpcApi.js";
import { formatDate } from "../utils/jobs.js";
import StatusBadge from "../components/StatusBadge.vue";
import ArtifactUploadForm from "../components/ArtifactUploadForm.vue";
import HpcIconTrash from "../components/HpcIconTrash.vue";

const statuses = ["CREATED", "UPLOADING", "REGISTERED", "COMMITTED", "FAILED"];

const items = ref([]);
const totalCount = ref(0);
const loading = ref(false);
const error = ref(null);
const statusFilter = ref("");
const offset = ref(0);
const limit = ref(25);
const showForm = ref(false);
const deleting = ref(false);

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

async function onDelete(artifact) {
  if (!confirm(`Delete artifact ${artifact.id?.substring(0, 8)}...?`)) return;
  deleting.value = true;
  try {
    await deleteArtifact(artifact.id);
    loadArtifacts();
  } catch (e) {
    error.value = e.message;
  } finally {
    deleting.value = false;
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
