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
                form.processor ? 'Select a profile...' : 'Select processor first'
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

        <div>
          <label class="block text-sm font-medium text-record-label mb-1"
            >Available Artifacts</label
          >
          <div class="flex gap-2">
            <InputSelect
              id="job-submit-input-artifacts"
              v-model="selectedArtifact"
              class="flex-1"
              :options="artifactOptions"
              placeholder="Select a committed artifact..."
            />
            <Button
              type="outline"
              size="small"
              :disabled="!selectedArtifact"
              @click="addArtifact"
            >
              Add
            </Button>
          </div>
          <p class="text-xs text-definition-list-term mt-1">
            Selected artifacts are shown by name and sent by ID in the
            submission payload order.
          </p>
        </div>

        <div v-if="form.inputs.length" class="mt-2">
          <ul class="flex flex-wrap gap-2">
            <li
              v-for="(id, idx) in form.inputs"
              :key="id"
              class="inline-flex items-center gap-1 px-3 py-1 rounded-full bg-content text-sm text-title"
            >
              <span>{{ artifactChipLabel(id) }}</span>
              <Button
                type="text"
                size="tiny"
                icon="cross"
                :icon-only="true"
                :aria-label="`Remove artifact ${artifactChipLabel(id)}`"
                :label="`Remove artifact ${artifactChipLabel(id)}`"
                @click="form.inputs.splice(idx, 1)"
              />
            </li>
          </ul>
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
import { ref, reactive, computed, onMounted, watch } from "vue";
import {
  submitJob,
  fetchArtifacts,
  fetchCapabilities,
} from "../composables/useHpcApi";
import Button from "../../../tailwind-components/app/components/Button.vue";
import Message from "../../../tailwind-components/app/components/Message.vue";
import InputString from "../../../tailwind-components/app/components/input/String.vue";
import InputSelect from "../../../tailwind-components/app/components/input/Select.vue";
import InputTextArea from "../../../tailwind-components/app/components/input/TextArea.vue";

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
const selectedArtifact = ref("");
const availableArtifacts = ref<any[]>([]);
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
  get: () =>
    form.timeoutSeconds != null ? String(form.timeoutSeconds) : "",
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

const artifactOptions = computed(() =>
  availableArtifacts.value.map((a) => ({
    value: a.id,
    label: `${a.name || a.id?.substring(0, 8)}${a.type ? ` (${a.type})` : ""}`,
  }))
);

watch(
  () => form.processor,
  () => {
    if (profiles.value.length && !profiles.value.includes(form.profile)) {
      form.profile = "";
    }
  }
);

function addArtifact() {
  if (
    selectedArtifact.value &&
    !form.inputs.includes(selectedArtifact.value)
  ) {
    form.inputs.push(selectedArtifact.value);
  }
  selectedArtifact.value = "";
}

function artifactChipLabel(id: string): string {
  const artifact = availableArtifacts.value.find((a) => a.id === id);
  if (!artifact) return id?.substring(0, 8) || id || "-";
  return artifact.name || artifact.id?.substring(0, 8) || "-";
}

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
    const payload: any = {
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
  } catch (e: any) {
    error.value = e.message;
  } finally {
    submitting.value = false;
  }
}

onMounted(() => {
  loadArtifacts();
  loadCapabilities();
});
</script>
