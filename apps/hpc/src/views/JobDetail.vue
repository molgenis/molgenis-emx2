<template>
  <div class="hpc-page-view">
    <div v-if="loading" class="hpc-surface hpc-feedback text-center">Loading job...</div>
    <div v-else-if="error" class="alert alert-danger hpc-feedback mb-0">{{ error }}</div>
    <template v-else-if="job">
      <section class="hpc-surface hpc-detail-card">
        <div class="hpc-detail-header">
          <div>
            <p class="hpc-detail-title">
              Job <code class="hpc-inline-code">{{ shortId(job.id) }}</code>
            </p>
            <p class="hpc-detail-subtitle">
              {{ job.processor }}{{ job.profile ? ` / ${job.profile}` : "" }} • created
              {{ formatDate(job.created_at) }}
            </p>
          </div>
          <div class="hpc-actions-tight">
            <StatusBadge :status="job.status" />
            <button
              class="btn btn-outline-danger btn-sm hpc-icon-btn hpc-icon-btn-label"
              title="Delete job"
              :disabled="deleting"
              @click="onDelete"
            >
              <HpcIconTrash />
              <span>Delete</span>
            </button>
          </div>
        </div>

        <div class="hpc-kv-grid">
          <div class="hpc-kv-item">
            <span class="hpc-kv-label">Job ID</span>
            <span class="hpc-kv-value"><code>{{ job.id }}</code></span>
          </div>
          <div class="hpc-kv-item">
            <span class="hpc-kv-label">Submitted By</span>
            <span class="hpc-kv-value">{{ job.submit_user || "-" }}</span>
          </div>
          <div class="hpc-kv-item">
            <span class="hpc-kv-label">Processor</span>
            <span class="hpc-kv-value">{{ job.processor || "-" }}</span>
          </div>
          <div class="hpc-kv-item">
            <span class="hpc-kv-label">Profile</span>
            <span class="hpc-kv-value">{{ job.profile || "-" }}</span>
          </div>
          <div class="hpc-kv-item">
            <span class="hpc-kv-label">Worker</span>
            <span class="hpc-kv-value">{{ job.worker_id || "-" }}</span>
          </div>
          <div class="hpc-kv-item">
            <span class="hpc-kv-label">Slurm Job ID</span>
            <span class="hpc-kv-value">{{ job.slurm_job_id || "-" }}</span>
          </div>
        </div>

        <div v-if="job.parameters" class="hpc-detail-section">
          <p class="hpc-section-title">Parameters</p>
          <p class="hpc-section-subtitle">JSON payload stored with the job submission.</p>
          <pre class="hpc-code-block"><code>{{ formatJson(job.parameters) }}</code></pre>
        </div>

        <div v-if="normalizedInputs.length" class="hpc-detail-section">
          <p class="hpc-section-title">Input Artifacts</p>
          <p class="hpc-section-subtitle">
            Artifacts referenced by this job at submission time.
          </p>
          <div class="hpc-table-wrap">
            <table class="table table-sm hpc-mini-table mb-0">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Type</th>
                  <th>Status</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="input in normalizedInputs" :key="input.id || input">
                  <td>{{ input.name || shortId(input.id) || input }}</td>
                  <td>
                    <span v-if="input.type" class="badge hpc-badge-chip">{{ input.type }}</span>
                    <span v-else>-</span>
                  </td>
                  <td>
                    <StatusBadge v-if="input.status" :status="input.status" />
                    <span v-else class="text-muted">-</span>
                  </td>
                  <td>
                    <router-link
                      v-if="input.id"
                      :to="`/artifacts/${input.id}`"
                      class="btn btn-outline-primary btn-sm"
                    >
                      View
                    </router-link>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </section>

      <section v-if="job.output_artifact_id || job.log_artifact_id" class="hpc-artifact-summary-grid">
        <div v-if="job.output_artifact_id" class="hpc-surface hpc-artifact-summary">
          <div class="hpc-artifact-summary-head">
            <div>
              <p class="hpc-artifact-summary-title">Output Artifact</p>
              <p class="hpc-artifact-summary-meta">Primary produced artifact for this run.</p>
            </div>
            <StatusBadge :status="job.output_artifact_id.status" />
          </div>
          <div class="hpc-meta-rows">
            <div class="hpc-meta-row">
              <span class="hpc-meta-row-label">Name</span>
              <span class="hpc-meta-row-value">{{ job.output_artifact_id.name || "-" }}</span>
            </div>
            <div class="hpc-meta-row">
              <span class="hpc-meta-row-label">ID</span>
              <span class="hpc-meta-row-value"><code>{{ shortId(job.output_artifact_id.id) }}</code></span>
            </div>
            <div class="hpc-meta-row">
              <span class="hpc-meta-row-label">Type</span>
              <span class="hpc-meta-row-value">{{ job.output_artifact_id.type || "-" }}</span>
            </div>
            <div class="hpc-meta-row">
              <span class="hpc-meta-row-label">Residence</span>
              <span class="hpc-meta-row-value">{{ job.output_artifact_id.residence || "-" }}</span>
            </div>
          </div>
          <div class="hpc-artifact-summary-footer">
            <router-link
              :to="`/artifacts/${job.output_artifact_id.id}`"
              class="btn btn-outline-primary btn-sm"
            >
              View Details
            </router-link>
          </div>
        </div>

        <div v-if="job.log_artifact_id" class="hpc-surface hpc-artifact-summary">
          <div class="hpc-artifact-summary-head">
            <div>
              <p class="hpc-artifact-summary-title">Log Artifact</p>
              <p class="hpc-artifact-summary-meta">Execution logs captured and uploaded by the worker.</p>
            </div>
            <StatusBadge :status="job.log_artifact_id.status" />
          </div>
          <div class="hpc-meta-rows">
            <div class="hpc-meta-row">
              <span class="hpc-meta-row-label">Name</span>
              <span class="hpc-meta-row-value">{{ job.log_artifact_id.name || "-" }}</span>
            </div>
            <div class="hpc-meta-row">
              <span class="hpc-meta-row-label">ID</span>
              <span class="hpc-meta-row-value"><code>{{ shortId(job.log_artifact_id.id) }}</code></span>
            </div>
            <div class="hpc-meta-row">
              <span class="hpc-meta-row-label">Type</span>
              <span class="hpc-meta-row-value">{{ job.log_artifact_id.type || "-" }}</span>
            </div>
            <div class="hpc-meta-row">
              <span class="hpc-meta-row-label">Residence</span>
              <span class="hpc-meta-row-value">{{ job.log_artifact_id.residence || "-" }}</span>
            </div>
          </div>
          <div class="hpc-artifact-summary-footer">
            <router-link
              :to="`/artifacts/${job.log_artifact_id.id}`"
              class="btn btn-outline-primary btn-sm"
            >
              View Details
            </router-link>
          </div>
        </div>
      </section>

      <section class="hpc-surface hpc-table-card">
        <div class="hpc-table-section-header">
          <div>
            <p class="hpc-table-section-header-title">Transition History</p>
            <p class="hpc-table-section-header-subtitle">
              Lifecycle events recorded for this job.
            </p>
          </div>
          <small class="hpc-muted-note">{{ transitions.length }} entries</small>
        </div>
        <div class="hpc-table-wrap hpc-transition-table-wrap">
          <table class="table table-sm mb-0 hpc-table-compact">
            <thead>
              <tr>
                <th>Time</th>
                <th>From</th>
                <th>To</th>
                <th>Worker</th>
                <th>Detail</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="t in transitions" :key="t.id">
                <td>{{ formatDate(t.timestamp) }}</td>
                <td>
                  <StatusBadge v-if="t.from_status" :status="t.from_status" />
                  <span v-else class="text-muted">-</span>
                </td>
                <td><StatusBadge :status="t.to_status" /></td>
                <td>{{ t.worker_id || "-" }}</td>
                <td class="hpc-transition-detail-cell">{{ t.detail || "-" }}</td>
              </tr>
              <tr v-if="!transitions.length">
                <td colspan="5" class="text-center text-muted py-4">No transitions recorded</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </template>
    <div v-else class="alert alert-warning hpc-feedback mb-0">Job not found.</div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import { fetchJobDetail, deleteJob } from "../composables/useHpcApi.js";
import { formatDate } from "../utils/jobs.js";
import StatusBadge from "../components/StatusBadge.vue";
import HpcIconTrash from "../components/HpcIconTrash.vue";

const props = defineProps({
  id: { type: String, required: true },
});

const router = useRouter();
const job = ref(null);
const transitions = ref([]);
const loading = ref(true);
const error = ref(null);
const deleting = ref(false);

const normalizedInputs = computed(() => {
  const inputs = job.value?.inputs;
  if (!inputs || !Array.isArray(inputs)) return [];
  return inputs.map((item) => {
    if (typeof item === "string") return { id: item };
    return item;
  });
});

async function onDelete() {
  if (!confirm(`Delete job ${props.id}?`)) return;
  deleting.value = true;
  try {
    await deleteJob(props.id);
    router.push("/jobs");
  } catch (e) {
    error.value = e.message;
  } finally {
    deleting.value = false;
  }
}

function shortId(id) {
  return id?.substring?.(0, 8) || id || "-";
}

function formatJson(val) {
  if (!val) return "";
  if (typeof val === "string") {
    try {
      return JSON.stringify(JSON.parse(val), null, 2);
    } catch {
      return val;
    }
  }
  return JSON.stringify(val, null, 2);
}

onMounted(async () => {
  try {
    const result = await fetchJobDetail(props.id);
    job.value = result.job;
    transitions.value = result.transitions;
  } catch (e) {
    error.value = e.message;
  } finally {
    loading.value = false;
  }
});
</script>
