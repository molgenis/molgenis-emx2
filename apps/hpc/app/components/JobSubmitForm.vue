<template>
  <section class="bg-form rounded-lg border border-color-theme p-6 mb-3">
    <div class="flex items-start justify-between mb-4">
      <div>
        <p class="text-lg font-semibold text-title">Submit New Job</p>
        <p class="text-sm text-definition-list-term">
          Configure processor/profile and optionally attach committed input
          artifacts.
        </p>
      </div>
      <Button type="outline" size="small" @click="$emit('close')">
        Cancel
      </Button>
    </div>

    <Message v-if="error" id="job-submit-error" invalid class="mb-3">
      {{ error }}
    </Message>

    <div class="space-y-6">
      <section>
        <div class="mb-3">
          <p class="text-sm font-semibold text-title">Basics</p>
          <p class="text-xs text-definition-list-term">
            Required processor plus optional execution profile.
          </p>
        </div>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-record-label mb-1"
              >Processor *</label
            >
            <InputSelect
              v-if="processors.length"
              id="job-submit-processor-select"
              v-model="form.processor"
              :options="processors"
              placeholder="Select a processor..."
            />
            <InputString
              id="job-submit-processor"
              v-else
              v-model="form.processor"
              placeholder="e.g. e2e-test"
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-record-label mb-1"
              >Profile</label
            >
            <InputSelect
              v-if="processors.length"
              id="job-submit-profile-select"
              v-model="form.profile"
              :options="profiles"
              :disabled="!form.processor"
              :placeholder="
                form.processor
                  ? 'Select a profile...'
                  : 'Select processor first'
              "
            />
            <InputString
              id="job-submit-profile"
              v-else
              v-model="form.profile"
              placeholder="e.g. bash, gpu-medium"
            />
          </div>
        </div>
      </section>

      <section>
        <div class="mb-3">
          <p class="text-sm font-semibold text-title">Parameters</p>
          <p class="text-xs text-definition-list-term">
            Optional JSON payload passed to the processor as structured
            parameters.
          </p>
        </div>
        <div>
          <label class="block text-sm font-medium text-record-label mb-1"
            >Parameters (JSON)</label
          >
          <InputTextArea
            id="job-submit-parameters"
            v-model="form.parametersJson"
            placeholder='{"key":"value"}'
          />
          <p class="text-xs text-definition-list-term mt-1">
            Leave empty for no parameters. Invalid JSON will be rejected before
            submission.
          </p>
        </div>
      </section>

      <section>
        <div class="mb-3">
          <p class="text-sm font-semibold text-title">Limits</p>
          <p class="text-xs text-definition-list-term">
            Optional resource limits for the job.
          </p>
        </div>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-record-label mb-1"
              >Maximum duration (seconds)</label
            >
            <InputString
              id="job-submit-timeout"
              v-model="timeoutSecondsStr"
              placeholder="e.g. 3600"
            />
            <p class="text-xs text-definition-list-term mt-1">
              Total wall-clock limit including queue wait time. Leave empty for
              no limit.
            </p>
          </div>
        </div>
      </section>

      <section>
        <div class="mb-3">
          <p class="text-sm font-semibold text-title">Inputs</p>
          <p class="text-xs text-definition-list-term">
            Attach committed artifacts as job inputs (optional).
          </p>
        </div>

        <Button type="outline" size="small" @click="showArtifactPicker = true">
          + Add Artifact
        </Button>

        <ArtifactPickerModal
          v-model:visible="showArtifactPicker"
          :already-selected="form.inputs"
          @confirm="onArtifactsConfirmed"
        />

        <div v-if="form.inputs.length" class="mt-3 space-y-2">
          <div
            v-for="(id, idx) in form.inputs"
            :key="id"
            class="flex items-center justify-between p-3 rounded-lg border border-color-theme bg-content"
          >
            <div class="min-w-0">
              <p class="text-sm font-medium text-title truncate">
                {{ artifactDisplayName(id) }}
              </p>
              <p class="text-xs text-definition-list-term">
                {{ artifactDisplayMeta(id) }}
              </p>
            </div>
            <Button
              type="text"
              size="tiny"
              icon="cross"
              :icon-only="true"
              :aria-label="`Remove artifact ${artifactDisplayName(id)}`"
              :label="`Remove artifact ${artifactDisplayName(id)}`"
              @click="form.inputs.splice(idx, 1)"
            />
          </div>
        </div>
        <p v-else class="text-xs text-definition-list-term mt-1">
          No input artifacts selected.
        </p>
      </section>
    </div>

    <div class="flex justify-end mt-4">
      <Button
        type="primary"
        size="small"
        @click="handleSubmit"
        :disabled="submitting"
      >
        {{ submitting ? "Submitting..." : "Submit Job" }}
      </Button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import Button from "../../../tailwind-components/app/components/Button.vue";
import InputSelect from "../../../tailwind-components/app/components/input/Select.vue";
import InputString from "../../../tailwind-components/app/components/input/String.vue";
import InputTextArea from "../../../tailwind-components/app/components/input/TextArea.vue";
import Message from "../../../tailwind-components/app/components/Message.vue";
import type { NormalizedArtifact } from "../composables/useArtifactsApi";
import {
  fetchArtifacts,
  fetchCapabilities,
  submitJob,
} from "../composables/useHpcApi";
import ArtifactPickerModal from "./ArtifactPickerModal.vue";

const emit = defineEmits(["submitted", "close"]);

const form = reactive({
  processor: "",
  profile: "",
  parametersJson: "",
  timeoutSeconds: null as number | null,
  inputs: [] as string[],
});
const error = ref<string | null>(null);
const submitting = ref(false);
const showArtifactPicker = ref(false);
type SubmitJobPayload = {
  processor: string;
  profile?: string;
  parameters: unknown;
  inputs?: string[];
  timeout_seconds?: number;
};

const availableArtifacts = ref<NormalizedArtifact[]>([]);
const capabilities = ref<{ processor: string; profile: string }[]>([]);

const processors = computed(() => [
  ...new Set(capabilities.value.map((c) => c.processor)),
]);
const profiles = computed(() => {
  if (!form.processor) return [];
  return [
    ...new Set(
      capabilities.value
        .filter((c) => c.processor === form.processor)
        .map((c) => c.profile)
    ),
  ];
});

const timeoutSecondsStr = computed({
  get: () => (form.timeoutSeconds != null ? String(form.timeoutSeconds) : ""),
  set: (val: string) => {
    const trimmed = val.trim();
    if (!trimmed) {
      form.timeoutSeconds = null;
    } else {
      const n = parseInt(trimmed, 10);
      form.timeoutSeconds = Number.isNaN(n) ? null : n;
    }
  },
});

function onArtifactsConfirmed(artifacts: NormalizedArtifact[]) {
  for (const a of artifacts) {
    if (!form.inputs.includes(a.id)) {
      form.inputs.push(a.id);
    }
    if (!availableArtifacts.value.find((x) => x.id === a.id)) {
      availableArtifacts.value.push(a);
    }
  }
  showArtifactPicker.value = false;
}

function artifactDisplayName(id: string): string {
  const a = availableArtifacts.value.find((x) => x.id === id);
  return a?.name || id.substring(0, 8);
}

function artifactDisplayMeta(id: string): string {
  const a = availableArtifacts.value.find((x) => x.id === id);
  if (!a) return "";
  const parts: string[] = [];
  if (a.type) parts.push(a.type);
  if (a.size_bytes) parts.push(formatSize(Number(a.size_bytes)));
  if (a.committed_at) parts.push(new Date(a.committed_at).toLocaleDateString());
  return parts.join(" \u00b7 ");
}

function formatSize(bytes: number): string {
  const n = Number(bytes || 0);
  if (n < 1024) return `${n} B`;
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`;
  if (n < 1024 * 1024 * 1024) return `${(n / (1024 * 1024)).toFixed(1)} MB`;
  return `${(n / (1024 * 1024 * 1024)).toFixed(2)} GB`;
}

watch(
  () => form.processor,
  () => {
    if (profiles.value.length && !profiles.value.includes(form.profile)) {
      form.profile = "";
    }
  }
);

async function loadArtifacts() {
  try {
    const result = await fetchArtifacts({ status: "COMMITTED", limit: 100 });
    availableArtifacts.value = result.items;
  } catch {
    // silently ignore — artifact selection is optional
  }
}

async function loadCapabilities() {
  try {
    capabilities.value = await fetchCapabilities();
  } catch {
    // silently ignore — falls back to free-text inputs
  }
}

async function handleSubmit() {
  error.value = null;
  if (!form.processor.trim()) {
    error.value = "Processor is required.";
    return;
  }
  let parameters = null;
  if (form.parametersJson.trim()) {
    try {
      parameters = JSON.parse(form.parametersJson);
    } catch {
      error.value = "Parameters must be valid JSON.";
      return;
    }
  }
  submitting.value = true;
  try {
    const payload: SubmitJobPayload = {
      processor: form.processor,
      profile: form.profile || undefined,
      parameters,
    };
    if (form.inputs.length) {
      payload.inputs = form.inputs;
    }
    if (form.timeoutSeconds != null && form.timeoutSeconds > 0) {
      payload.timeout_seconds = form.timeoutSeconds;
    }
    await submitJob(payload);
    form.processor = "";
    form.profile = "";
    form.parametersJson = "";
    form.timeoutSeconds = null;
    form.inputs = [];
    emit("submitted");
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : "Job submission failed.";
  } finally {
    submitting.value = false;
  }
}

onMounted(() => {
  loadArtifacts();
  loadCapabilities();
});
</script>
