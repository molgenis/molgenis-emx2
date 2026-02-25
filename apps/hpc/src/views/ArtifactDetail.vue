<template>
  <div class="hpc-page-view">
    <div v-if="loading" class="hpc-surface hpc-feedback text-center">Loading artifact...</div>
    <div v-else-if="error" class="alert alert-danger hpc-feedback mb-0">{{ error }}</div>
    <template v-else-if="artifact">
      <section class="hpc-surface hpc-detail-card">
        <div class="hpc-detail-header">
          <div>
            <p class="hpc-detail-title">
              Artifact <code class="hpc-inline-code">{{ shortId(artifact.id) }}</code>
            </p>
            <p class="hpc-detail-subtitle">
              {{ artifact.type || "blob" }} / {{ artifact.residence || "managed" }} &bull; created
              {{ formatDate(artifact.created_at) }}
            </p>
          </div>
          <div class="hpc-actions-tight">
            <StatusBadge :status="artifact.status" />
            <button
              class="btn btn-outline-danger btn-sm hpc-icon-btn hpc-icon-btn-label"
              title="Delete artifact"
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
            <span class="hpc-kv-label">Artifact ID</span>
            <span class="hpc-kv-value"><code>{{ artifact.id }}</code></span>
          </div>
          <div class="hpc-kv-item">
            <span class="hpc-kv-label">Name</span>
            <span class="hpc-kv-value">{{ artifact.name || "-" }}</span>
          </div>
          <div class="hpc-kv-item">
            <span class="hpc-kv-label">Type</span>
            <span class="hpc-kv-value">{{ artifact.type || "-" }}</span>
          </div>
          <div class="hpc-kv-item">
            <span class="hpc-kv-label">Residence</span>
            <span class="hpc-kv-value">{{ artifact.residence || "-" }}</span>
          </div>
          <div class="hpc-kv-item">
            <span class="hpc-kv-label">SHA-256</span>
            <span class="hpc-kv-value">
              <code v-if="artifact.sha256">{{ artifact.sha256 }}</code>
              <span v-else>-</span>
            </span>
          </div>
          <div class="hpc-kv-item">
            <span class="hpc-kv-label">Size</span>
            <span class="hpc-kv-value">{{ formatSize(artifact.size_bytes) }}</span>
          </div>
          <div v-if="artifact.content_url" class="hpc-kv-item">
            <span class="hpc-kv-label">Content URL</span>
            <span class="hpc-kv-value">{{ artifact.content_url }}</span>
          </div>
          <div class="hpc-kv-item">
            <span class="hpc-kv-label">Committed</span>
            <span class="hpc-kv-value">{{ formatDate(artifact.committed_at) }}</span>
          </div>
        </div>
      </section>

      <section v-if="provenance" class="hpc-surface hpc-detail-card">
        <div class="hpc-detail-header">
          <div>
            <p class="hpc-detail-title">Provenance</p>
            <p class="hpc-detail-subtitle">Lineage information captured when this artifact was produced.</p>
          </div>
        </div>

        <div class="hpc-kv-grid">
          <div v-if="provenance.job_id" class="hpc-kv-item">
            <span class="hpc-kv-label">Producing Job</span>
            <span class="hpc-kv-value">
              <router-link :to="`/jobs/${provenance.job_id}`">
                <code>{{ shortId(provenance.job_id) }}</code>
              </router-link>
              <span v-if="provenance.artifact_role" class="text-muted ms-1">({{ provenance.artifact_role }})</span>
            </span>
          </div>
          <div v-if="provenance.processor" class="hpc-kv-item">
            <span class="hpc-kv-label">Processor</span>
            <span class="hpc-kv-value">{{ provenance.processor }}{{ provenance.profile ? ` / ${provenance.profile}` : "" }}</span>
          </div>
          <div v-if="provenance.worker_id" class="hpc-kv-item">
            <span class="hpc-kv-label">Worker</span>
            <span class="hpc-kv-value">{{ provenance.worker_id }}</span>
          </div>
          <div v-if="provenance.created_by" class="hpc-kv-item">
            <span class="hpc-kv-label">Created By</span>
            <span class="hpc-kv-value">{{ provenance.created_by }}</span>
          </div>
          <div v-if="provenance.parameters_hash" class="hpc-kv-item">
            <span class="hpc-kv-label">Parameters Hash</span>
            <span class="hpc-kv-value"><code>{{ provenance.parameters_hash.substring(0, 16) }}...</code></span>
          </div>
        </div>

        <div v-if="provenance.input_artifact_ids?.length" class="hpc-detail-section">
          <p class="hpc-section-title">Input Artifacts</p>
          <p class="hpc-section-subtitle">Artifacts used as input to the producing job.</p>
          <div class="hpc-table-wrap">
            <table class="table table-sm hpc-mini-table mb-0">
              <thead>
                <tr>
                  <th>Artifact ID</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="inputId in provenance.input_artifact_ids" :key="inputId">
                  <td><code>{{ shortId(inputId) }}</code></td>
                  <td>
                    <router-link
                      :to="`/artifacts/${inputId}`"
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

      <section v-if="hasExtraMetadata" class="hpc-surface hpc-detail-card">
        <div class="hpc-detail-header">
          <div>
            <p class="hpc-detail-title">Metadata</p>
            <p class="hpc-detail-subtitle">Additional metadata stored with this artifact.</p>
          </div>
        </div>
        <pre class="hpc-code-block"><code>{{ formatJson(extraMetadata) }}</code></pre>
      </section>

      <section class="hpc-surface hpc-table-card">
        <div class="hpc-table-section-header">
          <div>
            <p class="hpc-table-section-header-title">Files</p>
            <p class="hpc-table-section-header-subtitle">
              Content files stored in this artifact.
            </p>
          </div>
          <small class="hpc-muted-note">{{ files.length }} files</small>
        </div>
        <div class="hpc-table-wrap">
          <table class="table table-sm mb-0 hpc-table-compact">
            <thead>
              <tr>
                <th>Path</th>
                <th>SHA-256</th>
                <th>Size</th>
                <th>Content-Type</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="f in files" :key="f.id">
                <td><code>{{ f.path }}</code></td>
                <td>
                  <code v-if="f.sha256">{{ f.sha256?.substring(0, 12) }}...</code>
                  <span v-else>-</span>
                </td>
                <td>{{ formatSize(f.size_bytes) }}</td>
                <td>{{ f.content_type || "-" }}</td>
                <td>
                  <button
                    v-if="artifact.residence === 'managed' && artifact.status === 'COMMITTED'"
                    class="btn btn-outline-primary btn-sm"
                    @click="onDownload(f.path)"
                  >
                    Download
                  </button>
                  <span v-else-if="artifact.residence === 'posix'" class="text-muted small">
                    {{ artifact.content_url }}/{{ f.path }}
                  </span>
                </td>
              </tr>
              <tr v-if="!files.length">
                <td colspan="5" class="text-center text-muted py-4">No files</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </template>
    <div v-else class="alert alert-warning hpc-feedback mb-0">Artifact not found.</div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import { fetchArtifactDetail, downloadArtifactFile, deleteArtifact } from "../composables/useHpcApi.js";
import { formatDate } from "../utils/jobs.js";
import StatusBadge from "../components/StatusBadge.vue";
import HpcIconTrash from "../components/HpcIconTrash.vue";

const props = defineProps({
  id: { type: String, required: true },
});

const router = useRouter();
const artifact = ref(null);
const files = ref([]);
const loading = ref(true);
const error = ref(null);
const deleting = ref(false);

const PROVENANCE_KEYS = new Set([
  "job_id", "processor", "profile", "worker_id",
  "artifact_role", "created_by", "input_artifact_ids", "parameters_hash",
]);

const provenance = computed(() => {
  const meta = artifact.value?.metadata;
  if (!meta || typeof meta !== "object" || !meta.job_id) return null;
  return meta;
});

const extraMetadata = computed(() => {
  const meta = artifact.value?.metadata;
  if (!meta || typeof meta !== "object") return meta;
  const extra = {};
  for (const [k, v] of Object.entries(meta)) {
    if (!PROVENANCE_KEYS.has(k)) extra[k] = v;
  }
  return extra;
});

const hasExtraMetadata = computed(() => {
  const extra = extraMetadata.value;
  if (!extra) return false;
  if (typeof extra === "object") return Object.keys(extra).length > 0;
  return true;
});

function shortId(id) {
  return id?.substring?.(0, 8) || id || "-";
}

function formatSize(bytes) {
  if (bytes == null) return "-";
  const n = Number(bytes);
  if (n < 1024) return `${n} B`;
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`;
  return `${(n / (1024 * 1024)).toFixed(1)} MB`;
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

async function onDownload(filePath) {
  try {
    await downloadArtifactFile(props.id, filePath);
  } catch (e) {
    error.value = e.message;
  }
}

async function onDelete() {
  if (!confirm(`Delete artifact ${props.id}?`)) return;
  deleting.value = true;
  try {
    await deleteArtifact(props.id);
    router.push("/artifacts");
  } catch (e) {
    error.value = e.message;
  } finally {
    deleting.value = false;
  }
}

onMounted(async () => {
  try {
    const result = await fetchArtifactDetail(props.id);
    artifact.value = result.artifact;
    files.value = result.files;
  } catch (e) {
    error.value = e.message;
  } finally {
    loading.value = false;
  }
});
</script>
