<template>
  <div class="card mb-3">
    <div class="card-header d-flex justify-content-between align-items-center">
      <strong>Upload New Artifact</strong>
      <button class="btn btn-sm btn-outline-secondary" @click="$emit('close')">Cancel</button>
    </div>
    <div class="card-body">
      <div v-if="error" class="alert alert-danger">{{ error }}</div>

      <!-- Step 1: Metadata -->
      <div v-if="step === 'metadata'">
        <div class="mb-2">
          <label class="form-label">Type</label>
          <select v-model="form.type" class="form-select form-select-sm">
            <option value="blob">blob</option>
            <option value="tabular">tabular</option>
            <option value="model">model</option>
            <option value="dataset">dataset</option>
            <option value="report">report</option>
          </select>
        </div>
        <div class="mb-2">
          <label class="form-label">Format</label>
          <input v-model="form.format" class="form-control form-control-sm" placeholder="e.g. csv, parquet, tar.gz" />
        </div>
        <div class="mb-3">
          <label class="form-label">Files *</label>
          <input type="file" class="form-control form-control-sm" multiple @change="onFilesSelected" />
          <small v-if="selectedFiles.length" class="text-muted">
            {{ selectedFiles.length }} file(s) selected
          </small>
        </div>
        <button
          class="btn btn-primary btn-sm"
          :disabled="!selectedFiles.length"
          @click="startUpload"
        >
          Upload
        </button>
      </div>

      <!-- Step 2: Uploading -->
      <div v-else-if="step === 'uploading'">
        <p>Uploading {{ uploadedCount }} / {{ selectedFiles.length }} files...</p>
        <div class="progress mb-2">
          <div
            class="progress-bar"
            :style="{ width: progressPercent + '%' }"
          >
            {{ progressPercent }}%
          </div>
        </div>
        <small v-if="currentFile" class="text-muted">{{ currentFile }}</small>
      </div>

      <!-- Step 3: Done -->
      <div v-else-if="step === 'done'">
        <div class="alert alert-success mb-2">
          Artifact <code>{{ artifactId?.substring(0, 8) }}</code> committed successfully.
        </div>
        <button class="btn btn-outline-primary btn-sm" @click="$emit('created')">
          Close
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from "vue";
import { createArtifact, uploadArtifactFile, commitArtifact } from "../composables/useHpcApi.js";

const emit = defineEmits(["created", "close"]);

const form = reactive({
  type: "blob",
  format: "",
});
const selectedFiles = ref([]);
const step = ref("metadata"); // metadata | uploading | done
const error = ref(null);
const artifactId = ref(null);
const uploadedCount = ref(0);
const currentFile = ref("");

const progressPercent = computed(() => {
  if (!selectedFiles.value.length) return 0;
  return Math.round((uploadedCount.value / selectedFiles.value.length) * 100);
});

function onFilesSelected(e) {
  selectedFiles.value = Array.from(e.target.files);
}

async function startUpload() {
  error.value = null;
  step.value = "uploading";
  uploadedCount.value = 0;

  try {
    // Create artifact
    const result = await createArtifact({
      type: form.type,
      format: form.format || undefined,
    });
    artifactId.value = result.id;

    // Upload files and compute overall SHA-256
    let totalSize = 0;

    for (const file of selectedFiles.value) {
      currentFile.value = file.name;
      await uploadArtifactFile(artifactId.value, file);
      totalSize += file.size;
      uploadedCount.value++;
    }

    // Compute overall SHA-256 from file contents
    const hashBuffer = await computeOverallSha256(selectedFiles.value);

    // Commit
    await commitArtifact(artifactId.value, {
      sha256: hashBuffer,
      size_bytes: totalSize,
    });

    step.value = "done";
  } catch (e) {
    error.value = e.message;
    step.value = "metadata";
  }
}

async function computeOverallSha256(files) {
  // Concatenate all file contents and hash
  const parts = [];
  for (const file of files) {
    const buf = await file.arrayBuffer();
    parts.push(new Uint8Array(buf));
  }
  const totalLength = parts.reduce((sum, p) => sum + p.length, 0);
  const combined = new Uint8Array(totalLength);
  let offset = 0;
  for (const part of parts) {
    combined.set(part, offset);
    offset += part.length;
  }
  const hashBuf = await crypto.subtle.digest("SHA-256", combined);
  return Array.from(new Uint8Array(hashBuf))
    .map((b) => b.toString(16).padStart(2, "0"))
    .join("");
}
</script>
