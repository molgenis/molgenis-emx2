<template>
  <div class="space-y-4">
    <section class="bg-form rounded-lg border border-color-theme p-6">
      <div class="flex items-start justify-between">
        <div>
          <p class="text-lg font-semibold text-title">Jobs</p>
          <p class="text-sm text-definition-list-term">
            Filter, inspect, cancel, and clean up submitted HPC runs.
          </p>
        </div>
        <div class="flex items-center gap-2">
          <span class="text-xs text-definition-list-term">
            Auto-refresh: 10s
          </span>
          <Button
            v-if="hasTerminalJobs"
            type="outline"
            size="tiny"
            :disabled="clearing"
            @click="onClearCompleted"
          >
            Clear Completed
          </Button>
          <Button
            type="primary"
            size="tiny"
            @click="showForm = !showForm"
          >
            {{ showForm ? "Hide Form" : "+ New Job" }}
          </Button>
        </div>
      </div>
    </section>

    <JobSubmitForm
      v-if="showForm"
      @submitted="onJobSubmitted"
      @close="showForm = false"
    />

    <div
      v-if="loading && !items.length"
      class="bg-form rounded-lg border border-color-theme p-6 text-center text-definition-list-term"
    >
      Loading jobs...
    </div>
    <Message v-else-if="error" id="jobs-page-error" invalid>
      {{ error }}
    </Message>
    <div v-else>
      <section class="bg-form rounded-lg border border-color-theme">
        <div class="p-4 border-b border-color-theme">
          <div class="w-56">
            <InputSelect
              id="jobs-status-filter"
              v-model="statusFilter"
              :options="statuses"
              placeholder="All Statuses"
            />
          </div>
        </div>
        <div class="overflow-x-auto">
          <table class="w-full text-sm text-table-row">
            <thead>
              <tr class="border-b border-color-theme">
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
                  colspan="7"
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
          <div class="flex gap-1">
            <Button
              type="outline"
              size="tiny"
              :disabled="offset === 0"
              @click="offset = Math.max(0, offset - limit)"
            >
              &laquo; Prev
            </Button>
            <Button
              type="outline"
              size="tiny"
              :disabled="offset + limit >= totalCount"
              @click="offset += limit"
            >
              Next &raquo;
            </Button>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from "vue";
import { navigateTo } from "#app/composables/router";
import { fetchJobs, deleteJob, cancelJob } from "../composables/useHpcApi";
import { JOB_STATUSES, isTerminal } from "../utils/protocol";
import { formatDate } from "../utils/jobs";
import Button from "../../../tailwind-components/app/components/Button.vue";
import Message from "../../../tailwind-components/app/components/Message.vue";
import InputSelect from "../../../tailwind-components/app/components/input/Select.vue";

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
const clearing = ref(false);
const hasTerminalJobs = ref(false);

let refreshInterval: ReturnType<typeof setInterval> | null = null;
let initialLoadDone = false;

function mergeJobs(nextJobs: any[]) {
  const previousById = new Map(items.value.map((job) => [job.id, job]));
  items.value = nextJobs.map((nextJob) => {
    const previous = previousById.get(nextJob.id);
    if (!previous) return nextJob;
    Object.assign(previous, nextJob);
    return previous;
  });
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
    hasTerminalJobs.value = result.items.some((j) => isTerminal(j.status));
  } catch (e: any) {
    error.value = e.message;
  } finally {
    if (!initialLoadDone) {
      loading.value = false;
      initialLoadDone = true;
    }
  }
}

async function onDelete(job: any) {
  if (!confirm(`Delete job ${job.id?.substring(0, 8)}...?`)) return;
  deleting.value = true;
  try {
    await deleteJob(job.id);
    loadJobs();
  } catch (e: any) {
    error.value = e.message;
  } finally {
    deleting.value = false;
  }
}

async function onCancel(job: any) {
  if (!confirm(`Cancel job ${job.id?.substring(0, 8)}...?`)) return;
  cancelling.value = true;
  try {
    await cancelJob(job.id);
    loadJobs();
  } catch (e: any) {
    error.value = e.message;
  } finally {
    cancelling.value = false;
  }
}

async function onClearCompleted() {
  const terminalJobs = items.value.filter((j) => isTerminal(j.status));
  if (!terminalJobs.length) return;
  if (!confirm(`Delete ${terminalJobs.length} completed/terminal job(s)?`))
    return;
  clearing.value = true;
  try {
    await Promise.all(terminalJobs.map((j) => deleteJob(j.id)));
    loadJobs();
  } catch (e: any) {
    error.value = e.message;
  } finally {
    clearing.value = false;
  }
}

function onJobSubmitted() {
  showForm.value = false;
  loadJobs();
}

watch(statusFilter, () => {
  offset.value = 0;
  loadJobs();
});

watch(offset, () => {
  loadJobs();
});

onMounted(() => {
  loadJobs();
  refreshInterval = setInterval(() => loadJobs({ background: true }), 10000);
});

onUnmounted(() => {
  if (refreshInterval) clearInterval(refreshInterval);
});
</script>
