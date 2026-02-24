<template>
  <section class="hpc-form-shell mb-3">
    <div class="hpc-form-header">
      <div>
        <p class="hpc-form-title">Upload New Artifact</p>
        <p class="hpc-form-subtitle">
          Create metadata, upload one or more files, then commit the artifact.
        </p>
      </div>
      <button class="btn btn-sm btn-outline-secondary" @click="$emit('close')">Cancel</button>
    </div>

    <div v-if="error" class="alert alert-danger mb-3">{{ error }}</div>

    <div v-if="step === 'metadata'" class="hpc-form-stack">
      <section class="hpc-form-section">
        <div class="hpc-form-section-head">
          <p class="hpc-form-section-title">Basics</p>
          <p class="hpc-form-section-subtitle">Name is required; type is optional but useful for filtering.</p>
        </div>
        <div class="hpc-form-grid hpc-form-grid-2">
          <div class="hpc-form-field">
            <label class="form-label">Name *</label>
            <input
              v-model="form.name"
              class="form-control form-control-sm"
              placeholder="e.g. my-dataset-v1"
            />
          </div>
          <div class="hpc-form-field">
            <label class="form-label">Type</label>
            <input
              v-model="form.type"
              class="form-control form-control-sm"
              placeholder="e.g. csv, parquet, log, model"
            />
          </div>
        </div>
      </section>

      <section class="hpc-form-section">
        <div class="hpc-form-section-head">
          <p class="hpc-form-section-title">Files</p>
          <p class="hpc-form-section-subtitle">
            Select one or more files. They upload first, then the artifact is committed with a combined hash.
          </p>
        </div>
        <div class="hpc-form-field">
          <label class="form-label">Files *</label>
          <input type="file" class="form-control form-control-sm" multiple @change="onFilesSelected" />
          <small class="hpc-field-help">
            Multi-file uploads are supported. Large files may take longer while the hash is computed before commit.
          </small>
        </div>
        <ul v-if="selectedFiles.length" class="hpc-file-list">
          <li v-for="file in selectedFiles" :key="`${file.name}-${file.size}`">
            {{ file.name }} ({{ formatSize(file.size) }})
          </li>
        </ul>
      </section>

      <div class="hpc-form-actions">
        <button class="btn btn-primary btn-sm" :disabled="!canSubmit" @click="startUpload">
          Upload Artifact
        </button>
      </div>
    </div>

    <div v-else-if="step === 'uploading'" class="hpc-form-stack">
      <section class="hpc-form-section">
        <div class="hpc-form-section-head">
          <p class="hpc-form-section-title">Uploading</p>
          <p class="hpc-form-section-subtitle">
            Sending files sequentially and committing when all uploads complete.
          </p>
        </div>
        <div class="hpc-progress-label">
          <span>Uploading {{ uploadedCount }} / {{ selectedFiles.length }} files</span>
          <strong>{{ progressPercent }}%</strong>
        </div>
        <div class="progress mb-2">
          <div class="progress-bar" :style="{ width: progressPercent + '%' }">
            {{ progressPercent }}%
          </div>
        </div>
        <small v-if="currentFile" class="hpc-field-help mb-0">Current file: {{ currentFile }}</small>
      </section>
    </div>

    <div v-else-if="step === 'done'" class="hpc-form-stack">
      <section class="hpc-form-section">
        <div class="alert alert-success mb-0">
          Artifact <code>{{ artifactId?.substring(0, 8) }}</code> committed successfully.
        </div>
      </section>
      <div class="hpc-form-actions">
        <button class="btn btn-outline-primary btn-sm" @click="$emit('created')">Close</button>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, reactive, computed } from "vue";
import { createArtifact, uploadArtifactFile, commitArtifact } from "../composables/useHpcApi.js";

const emit = defineEmits(["created", "close"]);

const form = reactive({
  name: "",
  type: "",
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

const canSubmit = computed(() => {
  return form.name.trim() && selectedFiles.value.length > 0;
});

function formatSize(bytes) {
  const n = Number(bytes || 0);
  if (n < 1024) return `${n} B`;
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`;
  return `${(n / (1024 * 1024)).toFixed(1)} MB`;
}

function onFilesSelected(e) {
  selectedFiles.value = Array.from(e.target.files);
}

async function startUpload() {
  error.value = null;

  try {
    step.value = "uploading";
    uploadedCount.value = 0;

    const result = await createArtifact({
      name: form.name.trim(),
      type: form.type.trim() || undefined,
    });
    artifactId.value = result.id;

    let totalSize = 0;
    for (const file of selectedFiles.value) {
      currentFile.value = file.name;
      await uploadArtifactFile(artifactId.value, file);
      totalSize += file.size;
      uploadedCount.value++;
    }

    const hashBuffer = await computeOverallSha256(selectedFiles.value);
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
