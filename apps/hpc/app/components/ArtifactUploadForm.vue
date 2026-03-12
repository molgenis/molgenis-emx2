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
  uploadArtifactFile,
  commitArtifact,
} from "../composables/useHpcApi";
import Button from "../../../tailwind-components/app/components/Button.vue";
import Message from "../../../tailwind-components/app/components/Message.vue";
import InputString from "../../../tailwind-components/app/components/input/String.vue";
import InputFiles from "../../../tailwind-components/app/components/input/Files.vue";

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
