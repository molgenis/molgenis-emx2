<template>
  <div class="hpc-page-view">
    <section class="hpc-surface hpc-toolbar-card">
      <div class="hpc-toolbar-row">
        <div>
          <p class="hpc-toolbar-title">Jobs</p>
          <p class="hpc-toolbar-subtitle">
            Filter, inspect, cancel, and clean up submitted HPC runs.
          </p>
        </div>
        <div class="hpc-actions-tight">
          <button
            v-if="hasTerminalJobs"
            class="btn btn-outline-secondary btn-sm"
            :disabled="clearing"
            @click="onClearCompleted"
          >
            Clear Completed
          </button>
          <button class="btn btn-primary btn-sm" @click="showForm = !showForm">
            {{ showForm ? "Hide Form" : "+ New Job" }}
          </button>
        </div>
      </div>
    </section>

    <JobSubmitForm
      v-if="showForm"
      class="hpc-surface"
      @submitted="onJobSubmitted"
      @close="showForm = false"
    />

    <div v-if="loading && !items.length" class="hpc-surface hpc-feedback text-center">
      Loading jobs...
    </div>
    <div v-else-if="error" class="alert alert-danger hpc-feedback mb-0">{{ error }}</div>
    <div v-else>
      <section class="hpc-surface hpc-table-card">
        <div class="hpc-table-controls">
          <div class="hpc-toolbar-controls">
            <select v-model="statusFilter" class="form-select form-select-sm">
              <option value="">All Statuses</option>
              <option v-for="s in statuses" :key="s" :value="s">{{ s }}</option>
            </select>
          </div>
        </div>
        <div class="hpc-table-wrap">
          <table class="table table-sm table-hover">
            <thead>
              <tr>
                <th>ID</th>
                <th>Processor</th>
                <th>Profile</th>
                <th>Status</th>
                <th>Created</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="job in items"
                :key="job.id"
                class="hpc-row-link"
                @click="$router.push(`/jobs/${job.id}`)"
              >
                <td><code class="hpc-inline-code">{{ job.id?.substring(0, 8) }}</code></td>
                <td>{{ job.processor }}</td>
                <td>{{ job.profile || "-" }}</td>
                <td><StatusBadge :status="job.status" /></td>
                <td>{{ formatDate(job.created_at) }}</td>
                <td>
                  <div class="hpc-actions-tight">
                    <button
                      v-if="isTerminal(job.status)"
                      class="btn btn-outline-danger btn-sm hpc-icon-btn"
                      title="Delete job"
                      :disabled="deleting"
                      @click.stop="onDelete(job)"
                    >
                      <HpcIconTrash />
                    </button>
                    <button
                      v-else
                      class="btn btn-outline-warning btn-sm"
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
                <td colspan="6" class="text-center text-muted py-4">No jobs found</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="hpc-table-footer">
          <small class="hpc-muted-note">
            Showing {{ items.length }} of {{ totalCount }} jobs
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
import { fetchJobs, deleteJob, cancelJob } from "../composables/useHpcApi.js";
import { JOB_STATUSES, formatDate, isTerminal } from "../utils/jobs.js";
import StatusBadge from "../components/StatusBadge.vue";
import JobSubmitForm from "../components/JobSubmitForm.vue";
import HpcIconTrash from "../components/HpcIconTrash.vue";

const statuses = JOB_STATUSES;

const items = ref([]);
const totalCount = ref(0);
const loading = ref(false);
const error = ref(null);
const statusFilter = ref("");
const offset = ref(0);
const limit = ref(25);
const showForm = ref(false);
const deleting = ref(false);
const cancelling = ref(false);
const clearing = ref(false);
const hasTerminalJobs = ref(false);

let refreshInterval = null;

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
  } catch (e) {
    error.value = e.message;
  } finally {
    loading.value = false;
  }
}

async function onDelete(job) {
  if (!confirm(`Delete job ${job.id?.substring(0, 8)}...?`)) return;
  deleting.value = true;
  try {
    await deleteJob(job.id);
    loadJobs();
  } catch (e) {
    error.value = e.message;
  } finally {
    deleting.value = false;
  }
}

async function onCancel(job) {
  if (!confirm(`Cancel job ${job.id?.substring(0, 8)}...?`)) return;
  cancelling.value = true;
  try {
    await cancelJob(job.id);
    loadJobs();
  } catch (e) {
    error.value = e.message;
  } finally {
    cancelling.value = false;
  }
}

async function onClearCompleted() {
  const terminalJobs = items.value.filter((j) => isTerminal(j.status));
  if (!terminalJobs.length) return;
  if (!confirm(`Delete ${terminalJobs.length} completed/terminal job(s)?`)) return;
  clearing.value = true;
  try {
    await Promise.all(terminalJobs.map((j) => deleteJob(j.id)));
    loadJobs();
  } catch (e) {
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
  clearInterval(refreshInterval);
});
</script>
