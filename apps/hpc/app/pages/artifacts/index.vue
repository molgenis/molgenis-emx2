<template>
  <div class="space-y-4">
    <section class="bg-form rounded-lg border border-color-theme p-6">
      <div class="flex items-start justify-between">
        <div>
          <p class="text-lg font-semibold text-title">Artifacts</p>
          <p class="text-sm text-definition-list-term">
            Browse uploaded outputs and inspect artifact metadata and file
            contents.
          </p>
        </div>
        <div class="flex items-center gap-2">
          <select
            v-model="statusFilter"
            class="rounded-md border border-input bg-input text-input px-3 py-1.5 text-sm focus:border-input-focused focus:ring-1 focus:ring-blue-500"
          >
            <option value="">All Statuses</option>
            <option v-for="s in statuses" :key="s" :value="s">{{ s }}</option>
          </select>
          <button
            class="px-3 py-1.5 text-sm font-medium bg-button-primary text-button-primary border border-button-primary rounded-md hover:bg-button-primary-hover hover:text-button-primary-hover hover:border-button-primary-hover"
            @click="showForm = !showForm"
          >
            {{ showForm ? "Hide Form" : "+ New Artifact" }}
          </button>
        </div>
      </div>
    </section>

    <ArtifactUploadForm
      v-if="showForm"
      @created="onArtifactCreated"
      @close="showForm = false"
    />

    <div
      v-if="loading && !items.length"
      class="bg-form rounded-lg border border-color-theme p-6 text-center text-definition-list-term"
    >
      Loading artifacts...
    </div>
    <div
      v-else-if="error"
      class="bg-red-500/10 border border-red-500/20 text-red-700 p-4 rounded-lg"
    >
      {{ error }}
    </div>
    <div v-else>
      <section class="bg-form rounded-lg border border-color-theme">
        <div class="overflow-x-auto">
          <table class="w-full text-sm text-table-row">
            <thead>
              <tr class="border-b border-color-theme">
                <th class="px-4 py-3 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider">
                  ID
                </th>
                <th class="px-4 py-3 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider">
                  Name
                </th>
                <th class="px-4 py-3 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider">
                  Type
                </th>
                <th class="px-4 py-3 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider">
                  Residence
                </th>
                <th class="px-4 py-3 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider">
                  Status
                </th>
                <th class="px-4 py-3 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider">
                  Size
                </th>
                <th class="px-4 py-3 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider">
                  Created
                </th>
                <th class="px-4 py-3 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider"></th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="a in items"
                :key="a.id"
                class="border-b border-color-theme hover:bg-hover transition-colors cursor-pointer"
                @click="navigateTo(`/artifacts/${a.id}`)"
              >
                <td class="px-4 py-3">
                  <code class="text-xs bg-content px-1.5 py-0.5 rounded">{{
                    a.id?.substring(0, 8)
                  }}</code>
                </td>
                <td class="px-4 py-3">
                  {{ a.name || a.id?.substring(0, 8) }}
                </td>
                <td class="px-4 py-3">{{ a.type || "-" }}</td>
                <td class="px-4 py-3">{{ a.residence || "-" }}</td>
                <td class="px-4 py-3">
                  <StatusBadge :status="a.status" />
                </td>
                <td class="px-4 py-3">{{ formatSize(a.size_bytes) }}</td>
                <td class="px-4 py-3">{{ formatDate(a.created_at) }}</td>
                <td class="px-4 py-3">
                  <button
                    class="p-1.5 text-red-500 hover:bg-red-500/10 rounded w-6 h-6"
                    title="Delete artifact"
                    :disabled="deleting"
                    @click.stop="onDelete(a)"
                  >
                    <HpcIconTrash />
                  </button>
                </td>
              </tr>
              <tr v-if="!items.length">
                <td colspan="8" class="px-4 py-8 text-center text-definition-list-term">
                  No artifacts found
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div
          class="flex items-center justify-between px-4 py-3 border-t border-color-theme"
        >
          <span class="text-xs text-definition-list-term">
            Showing {{ items.length }} of {{ totalCount }} artifacts
          </span>
          <div class="flex gap-1">
            <button
              class="px-3 py-1 text-sm border border-color-theme rounded-md hover:bg-hover disabled:opacity-50 transition-colors"
              :disabled="offset === 0"
              @click="offset = Math.max(0, offset - limit)"
            >
              &laquo; Prev
            </button>
            <button
              class="px-3 py-1 text-sm border border-color-theme rounded-md hover:bg-hover disabled:opacity-50 transition-colors"
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

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from "vue";
import { navigateTo } from "#app/composables/router";
import { fetchArtifacts, deleteArtifact } from "../../composables/useHpcApi";
import { formatDate } from "../../utils/jobs";

const statuses = [
  "CREATED",
  "UPLOADING",
  "REGISTERED",
  "COMMITTED",
  "FAILED",
];

const items = ref<any[]>([]);
const totalCount = ref(0);
const loading = ref(false);
const error = ref<string | null>(null);
const statusFilter = ref("");
const offset = ref(0);
const limit = ref(25);
const showForm = ref(false);
const deleting = ref(false);

let refreshInterval: ReturnType<typeof setInterval> | null = null;

function formatSize(bytes: number | null | undefined): string {
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
  } catch (e: any) {
    error.value = e.message;
  } finally {
    loading.value = false;
  }
}

async function onDelete(artifact: any) {
  if (!confirm(`Delete artifact ${artifact.id?.substring(0, 8)}...?`)) return;
  deleting.value = true;
  try {
    await deleteArtifact(artifact.id);
    loadArtifacts();
  } catch (e: any) {
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
  if (refreshInterval) clearInterval(refreshInterval);
});
</script>
