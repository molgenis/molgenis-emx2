<template>
  <section class="bg-form rounded-lg border border-color-theme p-6 mb-3">
    <div class="flex items-start justify-between mb-4">
      <div>
        <p class="text-lg font-semibold text-title">Upload New Artifact</p>
        <p class="text-sm text-definition-list-term">
          Create metadata, upload one or more files, then commit the artifact.
        </p>
      </div>
      <Button type="outline" size="small" @click="$emit('close')">
        Cancel
      </Button>
    </div>

    <Message v-if="error" id="artifact-upload-error" invalid class="mb-3">
      {{ error }}
    </Message>

    <!-- Step 1: Metadata -->
    <div v-if="step === 'metadata'" class="space-y-6">
      <section>
        <div class="mb-3">
          <p class="text-sm font-semibold text-title">Basics</p>
          <p class="text-xs text-definition-list-term">
            Name is required; type is optional but useful for filtering.
          </p>
        </div>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-record-label mb-1"
              >Name *</label
            >
            <InputString
              id="artifact-upload-name"
              v-model="form.name"
              placeholder="e.g. my-dataset-v1"
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-record-label mb-1"
              >Type</label
            >
            <InputString
              id="artifact-upload-type"
              v-model="form.type"
              placeholder="e.g. csv, parquet, log, model"
            />
          </div>
        </div>
      </section>

      <section>
        <div class="mb-3">
          <p class="text-sm font-semibold text-title">Files</p>
          <p class="text-xs text-definition-list-term">
            Select one or more files. They upload first, then the artifact is
            committed with a combined hash.
          </p>
        </div>
        <div>
          <label class="block text-sm font-medium text-record-label mb-1"
            >Files *</label
          >
          <InputFiles id="artifact-upload-files" v-model="selectedFiles" />
          <p class="text-xs text-definition-list-term mt-1">
            Multi-file uploads are supported. Large files may take longer while
            the hash is computed before commit.
          </p>
        </div>
        <ul v-if="selectedFiles.length" class="mt-2 space-y-1">
          <li
            v-for="file in selectedFiles"
            :key="`${file.name}-${file.size}`"
            class="text-sm text-record-label"
          >
            {{ file.name }} ({{ formatSize(file.size) }})
          </li>
        </ul>
      </section>

      <div class="flex justify-end">
        <Button
          type="primary"
          size="small"
          :disabled="!canSubmit"
          @click="startUpload"
        >
          Upload Artifact
        </Button>
      </div>
    </div>

    <!-- Step 2: Uploading -->
    <div v-else-if="step === 'uploading'" class="space-y-4">
      <section>
        <div class="flex items-start justify-between mb-3">
          <div>
            <p class="text-sm font-semibold text-title">
              {{ uploadPhase === "committing" ? "Committing artifact..." : "Uploading" }}
            </p>
            <p class="text-xs text-definition-list-term">
              {{ uploadPhase === "committing"
                ? "Finalizing artifact with integrity verification."
                : "Sending files sequentially and committing when all uploads complete." }}
            </p>
          </div>
          <Button
            v-if="uploadPhase === 'uploading'"
            type="outline"
            size="small"
            @click="cancelUpload"
          >
            Cancel Upload
          </Button>
        </div>

        <!-- Overall progress -->
        <div class="mb-4">
          <div class="flex justify-between text-sm text-record-label mb-1">
            <span>
              Overall: {{ completedFileCount }} / {{ selectedFiles.length }} files
              ({{ formatSize(overallBytesLoaded) }} / {{ formatSize(overallBytesTotal) }})
            </span>
            <strong>{{ overallPercent }}%</strong>
          </div>
          <div class="w-full bg-content rounded-full h-2.5">
            <div
              class="bg-blue-500 h-2.5 rounded-full transition-all duration-300"
              :style="{ width: overallPercent + '%' }"
            ></div>
          </div>
          <div
            v-if="uploadPhase === 'uploading' && uploadSpeed > 0"
            class="flex justify-between text-xs text-definition-list-term mt-1"
          >
            <span>{{ formatSpeed(uploadSpeed) }}</span>
            <span>{{ formatEta(estimatedSecondsRemaining) }} remaining</span>
          </div>
        </div>

        <!-- Per-file progress list -->
        <ul class="space-y-2">
          <li
            v-for="(fs, index) in fileStates"
            :key="`${fs.name}-${index}`"
            class="text-sm"
          >
            <div class="flex justify-between text-record-label mb-0.5">
              <span class="truncate mr-2">
                <span
                  v-if="fs.status === 'done'"
                  class="text-valid font-medium"
                >done</span>
                <span
                  v-else-if="fs.status === 'uploading'"
                  class="text-blue-500 font-medium"
                >uploading</span>
                <span
                  v-else-if="fs.status === 'retrying'"
                  class="text-yellow-500 font-medium"
                >retry {{ fs.attempt }}/3</span>
                <span
                  v-else-if="fs.status === 'failed'"
                  class="text-invalid font-medium"
                >failed</span>
                <span
                  v-else
                  class="text-definition-list-term"
                >pending</span>
                &mdash; {{ fs.name }}
              </span>
              <span class="whitespace-nowrap">
                {{ formatSize(fs.loaded) }} / {{ formatSize(fs.total) }}
              </span>
            </div>
            <div class="w-full bg-content rounded-full h-1.5">
              <div
                class="h-1.5 rounded-full transition-all duration-200"
                :class="{
                  'bg-blue-500': fs.status === 'uploading',
                  'bg-green-500': fs.status === 'done',
                  'bg-yellow-500': fs.status === 'retrying',
                  'bg-red-500': fs.status === 'failed',
                  'bg-gray-300': fs.status === 'pending',
                }"
                :style="{ width: fs.percent + '%' }"
              ></div>
            </div>
            <p
              v-if="fs.error"
              class="text-xs text-invalid mt-0.5"
            >
              {{ fs.error }}
            </p>
          </li>
        </ul>
      </section>
    </div>

    <!-- Step 3: Done -->
    <div v-else-if="step === 'done'" class="space-y-4">
      <section>
        <Message id="artifact-upload-success" valid>
          Artifact
          <code class="bg-green-500/20 px-1 rounded">{{
            artifactId?.substring(0, 8)
          }}</code>
          committed successfully.
        </Message>
      </section>
      <div class="flex justify-end">
        <Button type="outline" size="small" @click="$emit('created')">
          Close
        </Button>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from "vue";
import {
  createArtifact,
  uploadArtifactFileWithRetry,
  commitArtifact,
  computeTreeHash,
} from "../composables/useHpcApi";
import type { UploadHandle } from "../composables/useHpcApi";
import Button from "../../../tailwind-components/app/components/Button.vue";
import Message from "../../../tailwind-components/app/components/Message.vue";
import InputString from "../../../tailwind-components/app/components/input/String.vue";
import InputFiles from "../../../tailwind-components/app/components/input/Files.vue";

defineEmits(["created", "close"]);

interface FileUploadState {
  name: string;
  total: number;
  loaded: number;
  percent: number;
  status: "pending" | "uploading" | "retrying" | "done" | "failed";
  attempt: number;
  sha256: string | null;
  error: string | null;
}

const form = reactive({ name: "", type: "" });
const selectedFiles = ref<File[]>([]);
const step = ref<"metadata" | "uploading" | "done">("metadata");
const error = ref<string | null>(null);
const artifactId = ref<string | null>(null);
const uploadPhase = ref<"uploading" | "committing">("uploading");
const fileStates = ref<FileUploadState[]>([]);

// Speed / ETA tracking
const speedSamples = ref<Array<{ time: number; bytes: number }>>([]);
const uploadSpeed = ref(0); // bytes per second

// Current active upload handle for cancellation
let activeHandle: UploadHandle | null = null;
let cancelled = false;

const completedFileCount = computed(
  () => fileStates.value.filter((f) => f.status === "done").length
);

const overallBytesTotal = computed(() =>
  fileStates.value.reduce((sum, f) => sum + f.total, 0)
);

const overallBytesLoaded = computed(() =>
  fileStates.value.reduce((sum, f) => sum + f.loaded, 0)
);

const overallPercent = computed(() => {
  if (overallBytesTotal.value === 0) return 0;
  return Math.round(
    (overallBytesLoaded.value / overallBytesTotal.value) * 100
  );
});

const estimatedSecondsRemaining = computed(() => {
  if (uploadSpeed.value <= 0) return Infinity;
  const remaining = overallBytesTotal.value - overallBytesLoaded.value;
  return remaining / uploadSpeed.value;
});

const canSubmit = computed(
  () => form.name.trim() && selectedFiles.value.length > 0
);

function formatSize(bytes: number): string {
  const n = Number(bytes || 0);
  if (n < 1024) return `${n} B`;
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`;
  if (n < 1024 * 1024 * 1024) return `${(n / (1024 * 1024)).toFixed(1)} MB`;
  return `${(n / (1024 * 1024 * 1024)).toFixed(2)} GB`;
}

function formatSpeed(bytesPerSec: number): string {
  return `${formatSize(bytesPerSec)}/s`;
}

function formatEta(seconds: number): string {
  if (!isFinite(seconds) || seconds < 0) return "--";
  if (seconds < 60) return `${Math.ceil(seconds)}s`;
  if (seconds < 3600) return `${Math.floor(seconds / 60)}m ${Math.ceil(seconds % 60)}s`;
  const h = Math.floor(seconds / 3600);
  const m = Math.ceil((seconds % 3600) / 60);
  return `${h}h ${m}m`;
}

function updateSpeed(loaded: number) {
  const now = Date.now();
  speedSamples.value.push({ time: now, bytes: loaded });
  // Keep only samples from the last 3 seconds for a rolling average
  const cutoff = now - 3000;
  speedSamples.value = speedSamples.value.filter((s) => s.time >= cutoff);
  if (speedSamples.value.length >= 2) {
    const oldest = speedSamples.value[0];
    const newest = speedSamples.value[speedSamples.value.length - 1];
    const dt = (newest.time - oldest.time) / 1000;
    if (dt > 0) {
      uploadSpeed.value = (newest.bytes - oldest.bytes) / dt;
    }
  }
}

function cancelUpload() {
  cancelled = true;
  activeHandle?.abort();
  error.value = "Upload cancelled.";
  step.value = "metadata";
}

async function startUpload() {
  error.value = null;
  cancelled = false;
  activeHandle = null;
  speedSamples.value = [];
  uploadSpeed.value = 0;

  const files = selectedFiles.value;
  fileStates.value = files.map((f) => ({
    name: f.name,
    total: f.size,
    loaded: 0,
    percent: 0,
    status: "pending",
    attempt: 0,
    sha256: null,
    error: null,
  }));

  try {
    step.value = "uploading";
    uploadPhase.value = "uploading";

    // Create the artifact record on the server
    const result = await createArtifact({
      name: form.name.trim(),
      type: form.type.trim() || undefined,
    });
    artifactId.value = result.id;
    if (cancelled) return;

    // Upload files sequentially with XHR progress + retry.
    // Each file's SHA-256 is computed internally by the upload function
    // (for the Content-SHA256 header) and returned for tree hash computation.
    const fileHashes: Array<{ path: string; sha256: string }> = [];
    let cumulativeBytesCompleted = 0;

    for (let i = 0; i < files.length; i++) {
      if (cancelled) return;

      const file = files[i];
      const fs = fileStates.value[i];
      const filePath = file.name;

      fs.status = "uploading";
      fs.attempt = 1;

      const handle = uploadArtifactFileWithRetry(
        artifactId.value!,
        file,
        filePath,
        (progress) => {
          fs.loaded = progress.loaded;
          fs.total = progress.total;
          fs.percent = progress.total > 0
            ? Math.round((progress.loaded / progress.total) * 100)
            : 0;
          updateSpeed(cumulativeBytesCompleted + progress.loaded);
        },
        3 // maxAttempts
      );
      activeHandle = handle;

      try {
        const { sha256 } = await handle.promise;
        fs.status = "done";
        fs.loaded = fs.total;
        fs.percent = 100;
        fs.sha256 = sha256;
        fileHashes.push({ path: filePath, sha256 });
        cumulativeBytesCompleted += file.size;
      } catch (err: any) {
        if (err?.name === "AbortError") {
          // Cancelled — don't set error per file, cancelUpload() handles it
          return;
        }
        fs.status = "failed";
        fs.error = err.message || "Upload failed";
        throw new Error(`Failed to upload ${file.name}: ${fs.error}`);
      }
    }

    if (cancelled) return;

    // Phase 3: Compute tree hash from per-file hashes and commit
    uploadPhase.value = "committing";
    const treeHash = await computeTreeHash(fileHashes);
    const totalSize = files.reduce((sum, f) => sum + f.size, 0);

    await commitArtifact(artifactId.value!, {
      sha256: treeHash,
      size_bytes: totalSize,
    });

    step.value = "done";
  } catch (e: any) {
    if (!cancelled) {
      error.value = e.message;
      step.value = "metadata";
    }
  }
}
</script>
