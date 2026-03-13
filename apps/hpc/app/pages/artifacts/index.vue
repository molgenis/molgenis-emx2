<template>
  <div class="space-y-4">
    <section class="bg-form rounded-lg border border-color-theme p-6">
      <div class="flex items-start justify-between">
        <div>
          <p class="text-lg font-semibold text-title">Artifacts</p>
          <p class="text-sm text-definition-list-term flex items-center gap-3">
            <span>
              Browse uploaded outputs and inspect artifact metadata and file
              contents.
            </span>
            <span class="text-[11px] tracking-wide opacity-70">
              Auto-refresh every 10 seconds
            </span>
          </p>
        </div>
        <div class="flex items-center gap-2">
          <div class="w-44">
            <InputSelect
              id="artifacts-status-filter"
              v-model="statusFilter"
              :options="statuses"
              placeholder="All Statuses"
            />
          </div>
          <Button
            type="primary"
            size="small"
            @click="showForm = !showForm"
          >
            {{ showForm ? "Hide Form" : "+ New Artifact" }}
          </Button>
        </div>
      </div>
      <div
        v-if="hasSelection"
        class="mt-4 flex flex-wrap items-center gap-2 rounded-md border border-yellow-500/40 bg-yellow-500/10 px-3 py-2"
      >
        <span class="text-sm text-title">
          {{ selectedCount }} selected
          <span class="text-definition-list-term">({{ scopeLabel }})</span>
        </span>
        <Button
          type="outline"
          size="tiny"
          :disabled="bulkDeleting || deleting"
          @click="onDeleteSelected"
        >
          {{ selectAllMatching ? "Delete All Matching" : "Delete Selected" }}
        </Button>
        <Button
          type="outline"
          size="tiny"
          :disabled="bulkDeleting || deleting"
          @click="clearSelection"
        >
          Clear Selection
        </Button>
        <span v-if="bulkDeleting" class="text-xs text-definition-list-term">
          Deleting {{ bulkDeletedCount }} / {{ bulkTotalCount }}...
        </span>
      </div>
    </section>

    <ArtifactUploadForm
      v-if="showForm"
      @created="onArtifactCreated"
      @close="showForm = false"
    />

    <Message v-if="error" id="artifacts-page-error" invalid>
      {{ error }}
    </Message>
    <div v-else>
      <div
        v-if="loading && !items.length"
        class="bg-form rounded-lg border border-color-theme p-4 text-sm text-definition-list-term"
      >
        Loading artifacts...
      </div>
      <Message v-if="notice" id="artifacts-page-notice" valid>
        {{ notice }}
      </Message>
      <section class="bg-form rounded-lg border border-color-theme">
        <div
          v-if="showSelectMoreBanner"
          class="border-b border-color-theme bg-content/40 px-4 py-3"
        >
          <div class="flex flex-wrap items-center gap-x-2 gap-y-1">
            <span class="text-sm text-title">
              All {{ pageIds.length }} artifacts on this page are selected.
            </span>
            <Button
              type="text"
              size="tiny"
              class="px-0 py-0 text-button-outline hover:text-button-outline-hover underline underline-offset-2 whitespace-nowrap"
              :disabled="bulkDeleting || deleting"
              @click="selectAllMatchingResults"
            >
              Select all {{ totalCount }} matching artifacts
            </Button>
          </div>
        </div>
        <div
          v-else-if="selectAllMatching"
          class="border-b border-color-theme bg-content/40 px-4 py-3"
        >
          <div class="flex flex-wrap items-center gap-x-2 gap-y-1">
            <span class="text-sm text-title">
              All {{ selectedCount }} matching artifacts are selected.
            </span>
            <Button
              type="text"
              size="tiny"
              class="px-0 py-0 text-button-outline hover:text-button-outline-hover underline underline-offset-2 whitespace-nowrap"
              :disabled="bulkDeleting || deleting"
              @click="clearSelection"
            >
              Clear selection
            </Button>
          </div>
        </div>
        <div class="overflow-x-auto">
          <table class="w-full text-sm text-table-row">
            <thead>
              <tr class="border-b border-color-theme">
                <th
                  class="px-4 py-3 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider w-10"
                >
                  <label class="group inline-flex items-center cursor-pointer">
                    <input
                      ref="pageSelectCheckbox"
                      type="checkbox"
                      class="sr-only peer"
                      :checked="allPageSelected"
                      :disabled="!pageIds.length || bulkDeleting || deleting"
                      @click.stop
                      @change="
                        togglePageSelection(
                          ($event.target as HTMLInputElement).checked
                        )
                      "
                    />
                    <InputCheckboxIcon
                      :checked="allPageSelected"
                      :indeterminate="somePageSelected"
                      :disabled="!pageIds.length || bulkDeleting || deleting"
                    />
                  </label>
                </th>
                <th
                  class="px-4 py-3 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider"
                >
                  ID
                </th>
                <th
                  class="px-4 py-3 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider"
                >
                  Name
                </th>
                <th
                  class="px-4 py-3 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider"
                >
                  Type
                </th>
                <th
                  class="px-4 py-3 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider"
                >
                  Residence
                </th>
                <th
                  class="px-4 py-3 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider"
                >
                  Status
                </th>
                <th
                  class="px-4 py-3 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider"
                >
                  Size
                </th>
                <th
                  class="px-4 py-3 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider"
                >
                  Created
                </th>
                <th
                  class="px-4 py-3 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider"
                ></th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="a in items"
                :key="a.id"
                class="border-b border-color-theme hover:bg-hover transition-colors cursor-pointer"
                @click="navigateTo(`/artifacts/${a.id}`)"
              >
                <td class="px-4 py-3" @click.stop>
                  <label class="group inline-flex items-center cursor-pointer">
                    <input
                      type="checkbox"
                      class="sr-only peer"
                      :checked="isArtifactSelected(a.id)"
                      :disabled="bulkDeleting || deleting"
                      @change="
                        toggleArtifactSelection(
                          a.id,
                          ($event.target as HTMLInputElement).checked
                        )
                      "
                    />
                    <InputCheckboxIcon
                      :checked="isArtifactSelected(a.id)"
                      :disabled="bulkDeleting || deleting"
                    />
                  </label>
                </td>
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
                  <Button
                    type="outline"
                    size="tiny"
                    icon="trash"
                    :icon-only="true"
                    label="Delete artifact"
                    title="Delete artifact"
                    :disabled="deleting || bulkDeleting"
                    @click.stop="onDelete(a)"
                  />
                </td>
              </tr>
              <tr v-if="!items.length">
                <td
                  colspan="9"
                  class="px-4 py-8 text-center text-definition-list-term"
                >
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
          <HpcPagination
            :current-page="currentPage"
            :total-pages="totalPages"
            @update="onPageUpdate"
          />
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted, computed, nextTick } from "vue";
import { navigateTo } from "#app/composables/router";
import { fetchArtifacts, deleteArtifact } from "../../composables/useHpcApi";
import type { NormalizedArtifact } from "../../composables/useArtifactsApi";
import { formatDate } from "../../utils/jobs";
import Button from "../../../../tailwind-components/app/components/Button.vue";
import HpcPagination from "../../components/HpcPagination.vue";
import Message from "../../../../tailwind-components/app/components/Message.vue";
import InputSelect from "../../../../tailwind-components/app/components/input/Select.vue";
import InputCheckboxIcon from "../../../../tailwind-components/app/components/input/CheckboxIcon.vue";

const statuses = ["CREATED", "UPLOADING", "REGISTERED", "COMMITTED", "FAILED"];

const items = ref<NormalizedArtifact[]>([]);
const totalCount = ref(0);
const loading = ref(false);
const error = ref<string | null>(null);
const statusFilter = ref("");
const offset = ref(0);
const limit = ref(25);
const showForm = ref(false);
const deleting = ref(false);
const bulkDeleting = ref(false);
const bulkDeletedCount = ref(0);
const bulkTotalCount = ref(0);
const notice = ref<string | null>(null);
const selectedIds = ref<Set<string>>(new Set());
const excludedIds = ref<Set<string>>(new Set());
const selectAllMatching = ref(false);
const pageSelectCheckbox = ref<HTMLInputElement | null>(null);
const currentPage = computed(() => Math.floor(offset.value / limit.value) + 1);
const totalPages = computed(() =>
  Math.max(1, Math.ceil(totalCount.value / limit.value))
);

let refreshInterval: ReturnType<typeof setInterval> | null = null;
let initialLoadDone = false;

function toErrorMessage(error: unknown): string {
  return error instanceof Error ? error.message : String(error ?? "Unknown error");
}

function mergeArtifacts(nextItems: NormalizedArtifact[]) {
  const previousById = new Map(items.value.map((a) => [a.id, a]));
  items.value = nextItems.map((next) => {
    const previous = previousById.get(next.id);
    if (!previous) return next;
    return { ...previous, ...next };
  });
}

const pageIds = computed(() =>
  items.value.map((a) => String(a?.id || "")).filter(Boolean)
);

const selectedCount = computed(() => {
  if (selectAllMatching.value) {
    return Math.max(0, totalCount.value - excludedIds.value.size);
  }
  return selectedIds.value.size;
});

const hasSelection = computed(() => selectedCount.value > 0);

const scopeLabel = computed(() =>
  statusFilter.value ? `status=${statusFilter.value}` : "all statuses"
);

const allPageSelected = computed(() => {
  if (!pageIds.value.length) return false;
  return pageIds.value.every((id) => isArtifactSelected(id));
});

const somePageSelected = computed(() => {
  if (!pageIds.value.length) return false;
  const selectedOnPage = pageIds.value.filter((id) =>
    isArtifactSelected(id)
  ).length;
  return selectedOnPage > 0 && selectedOnPage < pageIds.value.length;
});

const showSelectMoreBanner = computed(
  () =>
    !selectAllMatching.value &&
    allPageSelected.value &&
    totalCount.value > pageIds.value.length
);

function formatSize(bytes: number | null | undefined): string {
  if (bytes == null) return "-";
  const n = Number(bytes);
  if (n < 1024) return `${n} B`;
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`;
  return `${(n / (1024 * 1024)).toFixed(1)} MB`;
}

function isArtifactSelected(id: string): boolean {
  if (selectAllMatching.value) return !excludedIds.value.has(id);
  return selectedIds.value.has(id);
}

function toggleArtifactSelection(id: string, checked: boolean) {
  if (selectAllMatching.value) {
    const nextExcluded = new Set(excludedIds.value);
    if (checked) nextExcluded.delete(id);
    else nextExcluded.add(id);
    excludedIds.value = nextExcluded;
    return;
  }
  const nextSelected = new Set(selectedIds.value);
  if (checked) nextSelected.add(id);
  else nextSelected.delete(id);
  selectedIds.value = nextSelected;
}

function togglePageSelection(checked: boolean) {
  if (selectAllMatching.value) {
    const nextExcluded = new Set(excludedIds.value);
    for (const id of pageIds.value) {
      if (checked) nextExcluded.delete(id);
      else nextExcluded.add(id);
    }
    excludedIds.value = nextExcluded;
    return;
  }
  const nextSelected = new Set(selectedIds.value);
  for (const id of pageIds.value) {
    if (checked) nextSelected.add(id);
    else nextSelected.delete(id);
  }
  selectedIds.value = nextSelected;
}

function selectAllMatchingResults() {
  selectAllMatching.value = true;
  selectedIds.value = new Set();
  excludedIds.value = new Set();
}

function clearSelection() {
  selectedIds.value = new Set();
  excludedIds.value = new Set();
  selectAllMatching.value = false;
}

async function collectAllMatchingArtifactIds(): Promise<string[]> {
  const ids: string[] = [];
  const pageSize = 200;
  let cursor = 0;
  while (true) {
    const result = await fetchArtifacts({
      status: statusFilter.value || undefined,
      limit: pageSize,
      offset: cursor,
    });
    if (!result.items.length) break;
    for (const artifact of result.items) {
      const id = String(artifact?.id || "");
      if (!id) continue;
      if (excludedIds.value.has(id)) continue;
      ids.push(id);
    }
    cursor += pageSize;
    if (cursor >= result.totalCount) break;
  }
  return ids;
}

async function deleteInBatches(
  ids: string[],
  batchSize = 8
): Promise<string[]> {
  const failedIds: string[] = [];
  bulkTotalCount.value = ids.length;
  bulkDeletedCount.value = 0;

  for (let i = 0; i < ids.length; i += batchSize) {
    const batch = ids.slice(i, i + batchSize);
    const results = await Promise.allSettled(
      batch.map((id) => deleteArtifact(id))
    );
    results.forEach((result, index) => {
      if (result.status === "fulfilled") {
        bulkDeletedCount.value += 1;
      } else {
        failedIds.push(batch[index]);
      }
    });
  }
  return failedIds;
}

async function loadArtifacts({
  background = false,
}: { background?: boolean } = {}) {
  if (!initialLoadDone && !background) loading.value = true;
  if (!background) error.value = null;
  try {
    const result = await fetchArtifacts({
      status: statusFilter.value || undefined,
      limit: limit.value,
      offset: offset.value,
    });
    if (background) {
      mergeArtifacts(result.items);
    } else {
      items.value = result.items;
    }
    totalCount.value = result.totalCount;
  } catch (e: unknown) {
    if (!background) error.value = toErrorMessage(e);
  } finally {
    if (!initialLoadDone) {
      loading.value = false;
      initialLoadDone = true;
    }
  }
}

async function onDeleteSelected() {
  if (!selectedCount.value) return;

  const label = selectAllMatching.value
    ? `${selectedCount.value} matching artifact(s)`
    : `${selectedCount.value} selected artifact(s)`;

  if (selectAllMatching.value) {
    const token = `DELETE ${selectedCount.value}`;
    const typed = prompt(
      `Dangerous action: delete ${label} in ${scopeLabel.value}.\nType "${token}" to confirm.`
    );
    if (typed !== token) return;
  } else if (!confirm(`Delete ${label}? This cannot be undone.`)) {
    return;
  }

  bulkDeleting.value = true;
  error.value = null;
  notice.value = null;
  try {
    const targetIds = selectAllMatching.value
      ? await collectAllMatchingArtifactIds()
      : Array.from(selectedIds.value);
    const failedIds = await deleteInBatches(targetIds);

    if (failedIds.length) {
      const preview = failedIds.slice(0, 5).join(", ");
      error.value =
        `Deleted ${targetIds.length - failedIds.length}/${
          targetIds.length
        } artifacts. ` +
        `Failed: ${failedIds.length}${preview ? ` (e.g. ${preview})` : ""}`;
    } else {
      notice.value = `Deleted ${targetIds.length} artifact(s).`;
    }
    clearSelection();
    await loadArtifacts();
  } catch (e: unknown) {
    error.value = toErrorMessage(e);
  } finally {
    bulkDeleting.value = false;
    bulkDeletedCount.value = 0;
    bulkTotalCount.value = 0;
  }
}

async function onDelete(artifact: NormalizedArtifact) {
  if (!confirm(`Delete artifact ${artifact.id?.substring(0, 8)}...?`)) return;
  deleting.value = true;
  notice.value = null;
  try {
    await deleteArtifact(artifact.id);
    clearSelection();
    await loadArtifacts();
  } catch (e: unknown) {
    error.value = toErrorMessage(e);
  } finally {
    deleting.value = false;
  }
}

function onArtifactCreated() {
  showForm.value = false;
  notice.value = null;
  loadArtifacts();
}

function onPageUpdate(page: number) {
  offset.value = Math.max(0, (page - 1) * limit.value);
}

watch([statusFilter], () => {
  clearSelection();
  notice.value = null;
  offset.value = 0;
  loadArtifacts();
});

watch(offset, loadArtifacts);

watch([allPageSelected, somePageSelected], () => {
  nextTick(() => {
    if (!pageSelectCheckbox.value) return;
    pageSelectCheckbox.value.indeterminate = somePageSelected.value;
  });
});

onMounted(() => {
  loadArtifacts();
  refreshInterval = setInterval(() => {
    if (bulkDeleting.value) return;
    loadArtifacts({ background: true });
  }, 10000);
});

onUnmounted(() => {
  if (refreshInterval) clearInterval(refreshInterval);
});
</script>
