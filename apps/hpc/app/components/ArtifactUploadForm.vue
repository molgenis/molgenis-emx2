<template>
  <section class="bg-form rounded-lg border border-color-theme p-6 mb-3">
    <div class="flex items-start justify-between mb-4">
      <div>
        <p class="text-lg font-semibold text-title">Upload New Artifact</p>
        <p class="text-sm text-definition-list-term">
          Create metadata, upload one or more files, then commit the artifact.
        </p>
      </div>
      <button
        class="px-3 py-1.5 text-sm border border-color-theme rounded-md text-record-label hover:bg-hover"
        @click="$emit('close')"
      >
        Cancel
      </button>
    </div>

    <div
      v-if="error"
      class="bg-red-500/10 border border-red-500/20 text-red-700 p-4 rounded-lg mb-3"
    >
      {{ error }}
    </div>

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
            <input
              v-model="form.name"
              class="w-full rounded-md border border-input bg-input text-input px-3 py-2 text-sm focus:border-input-focused focus:ring-1 focus:ring-blue-500"
              placeholder="e.g. my-dataset-v1"
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-record-label mb-1"
              >Type</label
            >
            <input
              v-model="form.type"
              class="w-full rounded-md border border-input bg-input text-input px-3 py-2 text-sm focus:border-input-focused focus:ring-1 focus:ring-blue-500"
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
          <input
            type="file"
            class="w-full rounded-md border border-input px-3 py-2 text-sm text-input file:mr-3 file:rounded-md file:border-0 file:bg-blue-50 file:px-3 file:py-1 file:text-sm file:font-medium file:text-blue-700 hover:file:bg-blue-100"
            multiple
            @change="onFilesSelected"
          />
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
        <button
          class="px-4 py-2 text-sm font-medium bg-button-primary text-button-primary border border-button-primary rounded-md hover:bg-button-primary-hover hover:text-button-primary-hover hover:border-button-primary-hover disabled:opacity-50"
          :disabled="!canSubmit"
          @click="startUpload"
        >
          Upload Artifact
        </button>
      </div>
    </div>

    <div v-else-if="step === 'uploading'" class="space-y-4">
      <section>
        <div class="mb-3">
          <p class="text-sm font-semibold text-title">Uploading</p>
          <p class="text-xs text-definition-list-term">
            Sending files sequentially and committing when all uploads complete.
          </p>
        </div>
        <div class="flex justify-between text-sm text-record-label mb-1">
          <span
            >Uploading {{ uploadedCount }} /
            {{ selectedFiles.length }} files</span
          >
          <strong>{{ progressPercent }}%</strong>
        </div>
        <div class="w-full bg-content rounded-full h-2.5">
          <div
            class="bg-blue-500 h-2.5 rounded-full transition-all duration-300"
            :style="{ width: progressPercent + '%' }"
          ></div>
        </div>
        <p v-if="currentFile" class="text-xs text-definition-list-term mt-1">
          Current file: {{ currentFile }}
        </p>
      </section>
    </div>

    <div v-else-if="step === 'done'" class="space-y-4">
      <section>
        <div
          class="bg-green-500/10 border border-green-500/20 text-green-800 p-4 rounded-lg"
        >
          Artifact
          <code class="bg-green-500/20 px-1 rounded">{{
            artifactId?.substring(0, 8)
          }}</code>
          committed successfully.
        </div>
      </section>
      <div class="flex justify-end">
        <button
          class="px-3 py-2 text-sm border border-button-outline text-button-outline rounded-md hover:bg-button-outline-hover hover:text-button-outline-hover"
          @click="$emit('created')"
        >
          Close
        </button>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from "vue";
import {
  createArtifact,
  uploadArtifactFile,
  commitArtifact,
} from "../composables/useHpcApi";

const emit = defineEmits(["created", "close"]);

const form = reactive({
  name: "",
  type: "",
});
const selectedFiles = ref<File[]>([]);
const step = ref<"metadata" | "uploading" | "done">("metadata");
const error = ref<string | null>(null);
const artifactId = ref<string | null>(null);
const uploadedCount = ref(0);
const currentFile = ref("");

const progressPercent = computed(() => {
  if (!selectedFiles.value.length) return 0;
  return Math.round((uploadedCount.value / selectedFiles.value.length) * 100);
});

const canSubmit = computed(() => {
  return form.name.trim() && selectedFiles.value.length > 0;
});

function formatSize(bytes: number): string {
  const n = Number(bytes || 0);
  if (n < 1024) return `${n} B`;
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`;
  return `${(n / (1024 * 1024)).toFixed(1)} MB`;
}

function onFilesSelected(e: Event) {
  const target = e.target as HTMLInputElement;
  selectedFiles.value = Array.from(target.files || []);
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
      await uploadArtifactFile(artifactId.value!, file);
      totalSize += file.size;
      uploadedCount.value++;
    }

    const hashBuffer = await computeOverallSha256(selectedFiles.value);
    await commitArtifact(artifactId.value!, {
      sha256: hashBuffer,
      size_bytes: totalSize,
    });

    step.value = "done";
  } catch (e: any) {
    error.value = e.message;
    step.value = "metadata";
  }
}

async function computeOverallSha256(files: File[]): Promise<string> {
  const parts: Uint8Array[] = [];
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
