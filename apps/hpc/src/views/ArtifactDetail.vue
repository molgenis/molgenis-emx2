<template>
  <div>
    <div v-if="loading" class="text-center py-4">Loading...</div>
    <div v-else-if="error" class="alert alert-danger">{{ error }}</div>
    <div v-else-if="artifact">
      <div class="card mb-3">
        <div class="card-header d-flex justify-content-between align-items-center">
          <strong>{{ artifact.name || "Artifact" }}</strong>
          <div class="d-flex align-items-center gap-2">
            <StatusBadge :status="artifact.status" />
            <button
              class="btn btn-outline-danger btn-sm"
              title="Delete artifact"
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
                <dt>ID</dt>
                <dd><code>{{ artifact.id }}</code></dd>
                <dt>Name</dt>
                <dd>{{ artifact.name || "-" }}</dd>
                <dt>Type</dt>
                <dd>{{ artifact.type || "-" }}</dd>
                <dt>Residence</dt>
                <dd>{{ artifact.residence || "-" }}</dd>
                <dt>SHA-256</dt>
                <dd><code v-if="artifact.sha256">{{ artifact.sha256 }}</code><span v-else>-</span></dd>
              </dl>
            </div>
            <div class="col-md-6">
              <dl>
                <dt>Size</dt>
                <dd>{{ formatSize(artifact.size_bytes) }}</dd>
                <dt>Content URL</dt>
                <dd>{{ artifact.content_url || "-" }}</dd>
                <dt>Created</dt>
                <dd>{{ formatDate(artifact.created_at) }}</dd>
                <dt>Committed</dt>
                <dd>{{ formatDate(artifact.committed_at) }}</dd>
              </dl>
            </div>
          </div>
          <div v-if="artifact.metadata" class="mb-3">
            <strong>Metadata</strong>
            <pre class="bg-light p-2 rounded mt-1"><code>{{ formatJson(artifact.metadata) }}</code></pre>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-header"><strong>Files ({{ files.length }})</strong></div>
        <div class="card-body p-0">
          <table class="table table-sm mb-0">
            <thead>
              <tr>
                <th>Path</th>
                <th>Role</th>
                <th>SHA-256</th>
                <th>Size</th>
                <th>Content-Type</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="f in files" :key="f.id">
                <td><code>{{ f.path }}</code></td>
                <td>{{ f.role || "-" }}</td>
                <td><code v-if="f.sha256">{{ f.sha256?.substring(0, 12) }}...</code><span v-else>-</span></td>
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
                <td colspan="6" class="text-center text-muted">No files</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
    <div v-else class="alert alert-warning">Artifact not found.</div>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { fetchArtifactDetail, downloadArtifactFile, deleteArtifact } from "../composables/useHpcApi.js";
import { formatDate } from "../utils/jobs.js";
import StatusBadge from "../components/StatusBadge.vue";

const props = defineProps({
  id: { type: String, required: true },
});

const router = useRouter();
const artifact = ref(null);
const files = ref([]);
const loading = ref(true);
const error = ref(null);
const deleting = ref(false);

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
