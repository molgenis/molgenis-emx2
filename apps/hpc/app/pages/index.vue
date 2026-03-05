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
          <button
            v-if="hasTerminalJobs"
            class="px-3 py-1.5 text-sm border border-color-theme rounded-md text-record-label hover:bg-hover disabled:opacity-50"
            :disabled="clearing"
            @click="onClearCompleted"
          >
            Clear Completed
          </button>
          <button
            class="px-3 py-1.5 text-sm font-medium bg-button-primary text-button-primary border border-button-primary rounded-md hover:bg-button-primary-hover hover:text-button-primary-hover hover:border-button-primary-hover"
            @click="showForm = !showForm"
          >
            {{ showForm ? "Hide Form" : "+ New Job" }}
          </button>
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
    <div
      v-else-if="error"
      class="bg-red-500/10 border border-red-500/20 text-red-700 p-4 rounded-lg"
    >
      {{ error }}
    </div>
    <div v-else>
      <section class="bg-form rounded-lg border border-color-theme">
        <div class="p-4 border-b border-color-theme">
          <select
            v-model="statusFilter"
            class="rounded-md border border-input bg-input text-input px-3 py-1.5 text-sm focus:border-input-focused focus:ring-1 focus:ring-blue-500"
          >
            <option value="">All Statuses</option>
            <option v-for="s in statuses" :key="s" :value="s">{{ s }}</option>
          </select>
        </div>
        <div class="overflow-x-auto">
          <table class="w-full text-sm text-table-row">
            <thead>
              <tr class="border-b border-color-theme">
                <th class="px-4 py-3 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider">
                  ID
                </th>
                <th class="px-4 py-3 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider">
                  Processor
                </th>
                <th class="px-4 py-3 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider">
                  Profile
                </th>
                <th class="px-4 py-3 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider">
                  Status
                </th>
                <th class="px-4 py-3 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider">
                  Created
                </th>
                <th class="px-4 py-3 text-left text-xs font-semibold text-table-column-header uppercase tracking-wider"></th>
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
                <td class="px-4 py-3">
                  <div class="flex items-center gap-2">
                    <button
                      v-if="isTerminal(job.status)"
                      class="p-1.5 text-red-500 hover:bg-red-500/10 rounded w-6 h-6"
                      title="Delete job"
                      :disabled="deleting"
                      @click.stop="onDelete(job)"
                    >
                      <HpcIconTrash />
                    </button>
                    <button
                      v-else
                      class="px-2 py-1 text-xs border border-yellow-500 text-yellow-800 rounded hover:bg-yellow-200/50 disabled:opacity-50"
                      title="Cancel job"
                      :disabled="cancelling"
                      @click.stop="onCancel(job)"
                    >
                      Cancel
                    </button>
                  </div>
                </td>
              </tr>
              <tr v-if="!items.length">
                <td colspan="6" class="px-4 py-8 text-center text-definition-list-term">
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
import {
  fetchJobs,
  deleteJob,
  cancelJob,
} from "../composables/useHpcApi";
import { JOB_STATUSES, isTerminal } from "../utils/protocol";
import { formatDate } from "../utils/jobs";

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

async function loadJobs() {
  loading.value = true;
  error.value = null;
  try {
    const result = await fetchJobs({
      status: statusFilter.value || undefined,
      limit: limit.value,
      offset: offset.value,
    });
    items.value = result.items;
    totalCount.value = result.totalCount;
    hasTerminalJobs.value = result.items.some((j) => isTerminal(j.status));
  } catch (e: any) {
    error.value = e.message;
  } finally {
    loading.value = false;
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

watch(offset, loadJobs);

onMounted(() => {
  loadJobs();
  refreshInterval = setInterval(loadJobs, 10000);
});

onUnmounted(() => {
  if (refreshInterval) clearInterval(refreshInterval);
});
</script>
