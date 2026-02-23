<template>
  <div>
    <div v-if="loading" class="text-center py-4">Loading...</div>
    <div v-else-if="error" class="alert alert-danger">{{ error }}</div>
    <div v-else-if="job">
      <div class="card mb-3">
        <div class="card-header d-flex justify-content-between align-items-center">
          <strong>Job {{ job.id }}</strong>
          <div class="d-flex align-items-center gap-2">
            <StatusBadge :status="job.status" />
            <button
              class="btn btn-outline-danger btn-sm"
              title="Delete job"
              :disabled="deleting"
              @click="onDelete"
            >
              &#x1f5d1; Delete
            </button>
          </div>
        </div>
        <div class="card-body">
          <div class="row">
            <div class="col-md-6">
              <dl>
                <dt>Processor</dt>
                <dd>{{ job.processor }}</dd>
                <dt>Profile</dt>
                <dd>{{ job.profile || "-" }}</dd>
                <dt>Submitted By</dt>
                <dd>{{ job.submit_user || "-" }}</dd>
                <dt>Worker</dt>
                <dd>{{ job.worker_id || "-" }}</dd>
                <dt>Slurm Job ID</dt>
                <dd>{{ job.slurm_job_id || "-" }}</dd>
              </dl>
            </div>
            <div class="col-md-6">
              <dl>
                <dt>Created</dt>
                <dd>{{ formatDate(job.created_at) }}</dd>
                <dt>Claimed</dt>
                <dd>{{ formatDate(job.claimed_at) }}</dd>
                <dt>Submitted</dt>
                <dd>{{ formatDate(job.submitted_at) }}</dd>
                <dt>Started</dt>
                <dd>{{ formatDate(job.started_at) }}</dd>
                <dt>Completed</dt>
                <dd>{{ formatDate(job.completed_at) }}</dd>
              </dl>
            </div>
          </div>

          <div v-if="job.parameters" class="mb-3">
            <strong>Parameters</strong>
            <pre class="bg-light p-2 rounded mt-1"><code>{{ formatJson(job.parameters) }}</code></pre>
          </div>
          <div v-if="job.inputs">
            <strong>Inputs</strong>
            <pre class="bg-light p-2 rounded mt-1"><code>{{ formatJson(job.inputs) }}</code></pre>
          </div>
        </div>
      </div>

      <div v-if="job.output_artifact_id" class="card mb-3">
        <div class="card-header d-flex justify-content-between align-items-center">
          <strong>Output Artifact</strong>
          <StatusBadge :status="job.output_artifact_id.status" />
        </div>
        <div class="card-body">
          <dl class="mb-0">
            <dt>Artifact ID</dt>
            <dd><code>{{ job.output_artifact_id.id?.substring(0, 8) }}</code></dd>
            <dt>Type</dt>
            <dd>{{ job.output_artifact_id.type || "-" }}</dd>
          </dl>
          <router-link
            :to="`/artifacts/${job.output_artifact_id.id}`"
            class="btn btn-outline-primary btn-sm"
          >
            View Details
          </router-link>
        </div>
      </div>

      <div class="card">
        <div class="card-header"><strong>Transition History</strong></div>
        <div class="card-body p-0">
          <table class="table table-sm mb-0">
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
                <td>{{ t.detail || "-" }}</td>
              </tr>
              <tr v-if="!transitions.length">
                <td colspan="5" class="text-center text-muted">No transitions recorded</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
    <div v-else class="alert alert-warning">Job not found.</div>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { fetchJobDetail, deleteJob } from "../composables/useHpcApi.js";
import { formatDate } from "../utils/jobs.js";
import StatusBadge from "../components/StatusBadge.vue";

const props = defineProps({
  id: { type: String, required: true },
});

const router = useRouter();
const job = ref(null);
const transitions = ref([]);
const loading = ref(true);
const error = ref(null);
const deleting = ref(false);

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
