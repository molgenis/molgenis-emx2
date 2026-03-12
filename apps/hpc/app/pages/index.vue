<template>
  <div class="space-y-4">
    <section class="bg-form rounded-lg border border-color-theme p-6">
      <div class="flex items-start justify-between">
        <div>
          <p class="text-lg font-semibold text-title">Jobs</p>
          <p class="text-sm text-definition-list-term flex items-center gap-3">
            <span>
              Filter, inspect, cancel, and clean up submitted HPC runs.
            </span>
            <span class="text-[11px] tracking-wide opacity-70">
              Auto-refresh every 10 seconds
            </span>
          </p>
        </div>
        <div class="flex items-center gap-2">
          <div class="w-44">
            <InputSelect
              id="jobs-status-filter"
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
            {{ showForm ? "Hide Form" : "+ New Job" }}
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

    <JobSubmitForm
      v-if="showForm"
      @submitted="onJobSubmitted"
      @close="showForm = false"
    />

    <Message v-if="error" id="jobs-page-error" invalid>
      {{ error }}
    </Message>
    <Message v-if="notice" id="jobs-page-notice" valid>
      {{ notice }}
    </Message>
    <div v-else>
      <div
        v-if="loading && !items.length"
        class="bg-form rounded-lg border border-color-theme p-4 text-sm text-definition-list-term"
      >
        Loading jobs...
      </div>
      <section class="bg-form rounded-lg border border-color-theme">
        <div
          v-if="showSelectMoreBanner"
          class="px-4 py-3 border-b border-color-theme text-sm text-definition-list-term"
        >
          All {{ pageSelectableIds.length }} terminal jobs on this page are selected.
          <Button
            type="text"
            size="tiny"
            class="ml-1 text-button-outline hover:text-button-outline-hover underline underline-offset-2"
            :disabled="bulkDeleting || deleting"
            @click="onSelectAllMatchingResults"
          >
            Select all {{ totalSelectableCount }} matching terminal jobs
          </Button>
          .
        </div>
        <div
          v-else-if="selectAllMatching"
          class="px-4 py-3 border-b border-color-theme text-sm text-definition-list-term"
        >
          All {{ selectedCount }} matching terminal jobs are selected.
          <Button
            type="text"
            size="tiny"
            class="ml-1 text-button-outline hover:text-button-outline-hover underline underline-offset-2"
            :disabled="bulkDeleting || deleting"
            @click="clearSelection"
          >
            Clear selection
          </Button>
          .
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
                      :disabled="!pageSelectableIds.length || bulkDeleting || deleting"
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
                      :disabled="!pageSelectableIds.length || bulkDeleting || deleting"
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
                  Processor
                </th>
                <th
                  class="px-4 py-3 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider"
                >
                  Profile
                </th>
                <th
                  class="px-4 py-3 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider"
                >
                  Status
                </th>
                <th
                  class="px-4 py-3 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider"
                >
                  Created
                </th>
                <th
                  class="px-4 py-3 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider"
                >
                  Updated
                </th>
                <th
                  class="px-4 py-3 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider"
                ></th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="job in items"
                :key="job.id"
                class="border-b border-color-theme hover:bg-hover transition-colors cursor-pointer"
                @click="navigateTo(`/jobs/${job.id}`)"
              >
                <td class="px-4 py-3" @click.stop>
                  <label class="group inline-flex items-center cursor-pointer">
                    <input
                      type="checkbox"
                      class="sr-only peer"
                      :checked="isJobSelected(job.id)"
                      :disabled="!isTerminal(job.status) || bulkDeleting || deleting"
                      @change="
                        toggleJobSelection(
                          job.id,
                          ($event.target as HTMLInputElement).checked
                        )
                      "
                    />
                    <InputCheckboxIcon
                      :checked="isJobSelected(job.id)"
                      :disabled="!isTerminal(job.status) || bulkDeleting || deleting"
                    />
                  </label>
                </td>
                <td class="px-4 py-3">
                  <code class="text-xs bg-content px-1.5 py-0.5 rounded">{{
                    job.id?.substring(0, 8)
                  }}</code>
                </td>
                <td class="px-4 py-3">{{ job.processor }}</td>
                <td class="px-4 py-3">{{ job.profile || "-" }}</td>
                <td class="px-4 py-3">
                  <StatusBadge :status="job.status" />
                </td>
                <td class="px-4 py-3">{{ formatDate(job.created_at) }}</td>
                <td class="px-4 py-3">{{ formatDate(job.updated_at) }}</td>
                <td class="px-4 py-3">
                  <div class="flex items-center gap-2">
                    <Button
                      v-if="isTerminal(job.status)"
                      type="outline"
                      size="tiny"
                      icon="trash"
                      :icon-only="true"
                      label="Delete job"
                      title="Delete job"
                      :disabled="deleting"
                      @click.stop="onDelete(job)"
                    />
                    <Button
                      v-else
                      type="outline"
                      size="tiny"
                      title="Cancel job"
                      :disabled="cancelling"
                      @click.stop="onCancel(job)"
                    >
                      Cancel
                    </Button>
                  </div>
                </td>
              </tr>
              <tr v-if="!items.length">
                <td
                  colspan="8"
                  class="px-4 py-8 text-center text-definition-list-term"
                >
                  No jobs found
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div
          class="flex items-center justify-between px-4 py-3 border-t border-color-theme"
        >
          <span class="text-xs text-definition-list-term">
            Showing {{ items.length }} of {{ totalCount }} jobs
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
import { fetchJobs, deleteJob, cancelJob } from "../composables/useHpcApi";
import { JOB_STATUSES, isTerminal } from "../utils/protocol";
import { formatDate } from "../utils/jobs";
import Button from "../../../tailwind-components/app/components/Button.vue";
import HpcPagination from "../components/HpcPagination.vue";
import Message from "../../../tailwind-components/app/components/Message.vue";
import InputSelect from "../../../tailwind-components/app/components/input/Select.vue";
import InputCheckboxIcon from "../../../tailwind-components/app/components/input/CheckboxIcon.vue";

const statuses = JOB_STATUSES;

const items = ref<any[]>([]);
const totalCount = ref(0);
const loading = ref(false);
const error = ref<string | null>(null);
const statusFilter = ref("");
const offset = ref(0);
const limit = ref(25);
const showForm = ref(false);
const deleting = ref(false);
const cancelling = ref(false);
const bulkDeleting = ref(false);
const bulkDeletedCount = ref(0);
const bulkTotalCount = ref(0);
const notice = ref<string | null>(null);
const totalSelectableCount = ref(0);
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

function mergeJobs(nextJobs: any[]) {
  const previousById = new Map(items.value.map((job) => [job.id, job]));
  items.value = nextJobs.map((nextJob) => {
    const previous = previousById.get(nextJob.id);
    if (!previous) return nextJob;
    return { ...previous, ...nextJob };
  });
}

const pageSelectableIds = computed(() =>
  items.value
    .filter((job) => isTerminal(job.status))
    .map((job) => String(job?.id || ""))
    .filter(Boolean)
);

const selectedCount = computed(() => {
  if (selectAllMatching.value) {
    return Math.max(0, totalSelectableCount.value - excludedIds.value.size);
  }
  return selectedIds.value.size;
});

const hasSelection = computed(() => selectedCount.value > 0);

const scopeLabel = computed(() =>
  statusFilter.value ? `status=${statusFilter.value}` : "current filter"
);

const allPageSelected = computed(() => {
  if (!pageSelectableIds.value.length) return false;
  return pageSelectableIds.value.every((id) => isJobSelected(id));
});

const somePageSelected = computed(() => {
  if (!pageSelectableIds.value.length) return false;
  const selectedOnPage = pageSelectableIds.value.filter((id) =>
    isJobSelected(id)
  ).length;
  return selectedOnPage > 0 && selectedOnPage < pageSelectableIds.value.length;
});

const showSelectMoreBanner = computed(
  () =>
    !selectAllMatching.value &&
    allPageSelected.value &&
    totalSelectableCount.value > pageSelectableIds.value.length
);

function isJobSelected(id: string): boolean {
  if (selectAllMatching.value) return !excludedIds.value.has(id);
  return selectedIds.value.has(id);
}

function toggleJobSelection(id: string, checked: boolean) {
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
    for (const id of pageSelectableIds.value) {
      if (checked) nextExcluded.delete(id);
      else nextExcluded.add(id);
    }
    excludedIds.value = nextExcluded;
    return;
  }
  const nextSelected = new Set(selectedIds.value);
  for (const id of pageSelectableIds.value) {
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

async function onSelectAllMatchingResults() {
  const ids = await collectAllMatchingJobIds();
  totalSelectableCount.value = ids.length;
  selectAllMatchingResults();
}

function clearSelection() {
  selectedIds.value = new Set();
  excludedIds.value = new Set();
  selectAllMatching.value = false;
}

async function collectAllMatchingJobIds(): Promise<string[]> {
  const ids: string[] = [];
  const pageSize = 200;
  let cursor = 0;
  while (true) {
    const result = await fetchJobs({
      status: statusFilter.value || undefined,
      limit: pageSize,
      offset: cursor,
    });
    if (!result.items.length) break;
    for (const job of result.items) {
      const id = String(job?.id || "");
      if (!id || !isTerminal(job.status)) continue;
      if (excludedIds.value.has(id)) continue;
      ids.push(id);
    }
    cursor += pageSize;
    if (cursor >= result.totalCount) break;
  }
  return ids;
}

async function deleteInBatches(ids: string[], batchSize = 8): Promise<string[]> {
  const failedIds: string[] = [];
  bulkTotalCount.value = ids.length;
  bulkDeletedCount.value = 0;
  for (let i = 0; i < ids.length; i += batchSize) {
    const batch = ids.slice(i, i + batchSize);
    const results = await Promise.allSettled(batch.map((id) => deleteJob(id)));
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

async function loadJobs({ background = false }: { background?: boolean } = {}) {
  if (!initialLoadDone && !background) loading.value = true;
  if (!background) error.value = null;
  try {
    const result = await fetchJobs({
      status: statusFilter.value || undefined,
      limit: limit.value,
      offset: offset.value,
    });
    mergeJobs(result.items);
    totalCount.value = result.totalCount;
    if (!selectAllMatching.value) {
      totalSelectableCount.value = result.items.filter((j) =>
        isTerminal(j.status)
      ).length;
    }
  } catch (e: any) {
    error.value = e.message;
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
    ? `${selectedCount.value} matching terminal job(s)`
    : `${selectedCount.value} selected terminal job(s)`;

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
      ? await collectAllMatchingJobIds()
      : Array.from(selectedIds.value);
    const failedIds = await deleteInBatches(targetIds);

    if (failedIds.length) {
      const preview = failedIds.slice(0, 5).join(", ");
      error.value =
        `Deleted ${targetIds.length - failedIds.length}/${targetIds.length} jobs. ` +
        `Failed: ${failedIds.length}${preview ? ` (e.g. ${preview})` : ""}`;
    } else {
      notice.value = `Deleted ${targetIds.length} job(s).`;
    }
    clearSelection();
    await loadJobs();
  } catch (e: any) {
    error.value = e.message;
  } finally {
    bulkDeleting.value = false;
    bulkDeletedCount.value = 0;
    bulkTotalCount.value = 0;
  }
}

async function onDelete(job: any) {
  if (!confirm(`Delete job ${job.id?.substring(0, 8)}...?`)) return;
  deleting.value = true;
  notice.value = null;
  try {
    await deleteJob(job.id);
    clearSelection();
    await loadJobs();
  } catch (e: any) {
    error.value = e.message;
  } finally {
    deleting.value = false;
  }
}

async function onCancel(job: any) {
  if (!confirm(`Cancel job ${job.id?.substring(0, 8)}...?`)) return;
  cancelling.value = true;
  notice.value = null;
  try {
    await cancelJob(job.id);
    await loadJobs();
  } catch (e: any) {
    error.value = e.message;
  } finally {
    cancelling.value = false;
  }
}

function onJobSubmitted() {
  showForm.value = false;
  notice.value = null;
  loadJobs();
}

function onPageUpdate(page: number) {
  offset.value = Math.max(0, (page - 1) * limit.value);
}

watch(statusFilter, () => {
  clearSelection();
  notice.value = null;
  offset.value = 0;
  loadJobs();
});

watch(offset, () => {
  loadJobs();
});

watch([allPageSelected, somePageSelected], () => {
  nextTick(() => {
    if (!pageSelectCheckbox.value) return;
    pageSelectCheckbox.value.indeterminate = somePageSelected.value;
  });
});

onMounted(() => {
  loadJobs();
  refreshInterval = setInterval(() => loadJobs({ background: true }), 10000);
});

onUnmounted(() => {
  if (refreshInterval) clearInterval(refreshInterval);
});
</script>
