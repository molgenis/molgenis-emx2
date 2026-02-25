<template>
  <section class="hpc-form-shell mb-3">
    <div class="hpc-form-header">
      <div>
        <p class="hpc-form-title">Submit New Job</p>
        <p class="hpc-form-subtitle">
          Configure processor/profile and optionally attach committed input artifacts.
        </p>
      </div>
      <button class="btn btn-sm btn-outline-secondary" @click="$emit('close')">Cancel</button>
    </div>

    <div v-if="error" class="alert alert-danger mb-3">{{ error }}</div>

    <div class="hpc-form-stack">
      <section class="hpc-form-section">
        <div class="hpc-form-section-head">
          <p class="hpc-form-section-title">Basics</p>
          <p class="hpc-form-section-subtitle">Required processor plus optional execution profile.</p>
        </div>
        <div class="hpc-form-grid hpc-form-grid-2">
          <div class="hpc-form-field">
            <label class="form-label">Processor *</label>
            <select
              v-if="processors.length"
              v-model="form.processor"
              class="form-select form-select-sm"
            >
              <option value="">Select a processor...</option>
              <option v-for="p in processors" :key="p" :value="p">{{ p }}</option>
            </select>
            <input
              v-else
              v-model="form.processor"
              class="form-control form-control-sm"
              placeholder="e.g. e2e-test"
            />
          </div>
          <div class="hpc-form-field">
            <label class="form-label">Profile</label>
            <select
              v-if="profiles.length"
              v-model="form.profile"
              class="form-select form-select-sm"
            >
              <option value="">Select a profile...</option>
              <option v-for="p in profiles" :key="p" :value="p">{{ p }}</option>
            </select>
            <input
              v-else
              v-model="form.profile"
              class="form-control form-control-sm"
              placeholder="e.g. bash, gpu-medium"
            />
          </div>
        </div>
      </section>

      <section class="hpc-form-section">
        <div class="hpc-form-section-head">
          <p class="hpc-form-section-title">Parameters</p>
          <p class="hpc-form-section-subtitle">
            Optional JSON payload passed to the processor as structured parameters.
          </p>
        </div>
        <div class="hpc-form-field">
          <label class="form-label">Parameters (JSON)</label>
          <textarea
            v-model="form.parametersJson"
            class="form-control form-control-sm"
            rows="4"
            placeholder='{"key":"value"}'
          ></textarea>
          <small class="hpc-field-help">
            Leave empty for no parameters. Invalid JSON will be rejected before submission.
          </small>
        </div>
      </section>

      <section class="hpc-form-section">
        <div class="hpc-form-section-head">
          <p class="hpc-form-section-title">Inputs</p>
          <p class="hpc-form-section-subtitle">
            Attach committed artifacts as job inputs (optional).
          </p>
        </div>

        <div class="hpc-form-field">
          <label class="form-label">Available Artifacts</label>
          <div class="hpc-input-pair hpc-input-pair-compact">
            <select v-model="selectedArtifact" class="form-select form-select-sm">
              <option value="">Select a committed artifact...</option>
              <option v-for="a in availableArtifacts" :key="a.id" :value="a.id">
                {{ a.name || a.id?.substring(0, 8) }}{{ a.type ? ` (${a.type})` : "" }}
              </option>
            </select>
            <button
              class="btn btn-outline-primary btn-sm"
              :disabled="!selectedArtifact"
              @click="addArtifact"
            >
              Add
            </button>
          </div>
          <small class="hpc-field-help">
            Selected artifacts are shown by name and sent by ID in the submission payload order.
          </small>
        </div>

        <div v-if="form.inputs.length" class="mt-2">
          <ul class="hpc-chip-list">
            <li v-for="(id, idx) in form.inputs" :key="id" class="hpc-chip">
              <span>{{ artifactChipLabel(id) }}</span>
              <button
                type="button"
                class="hpc-chip-remove"
                :aria-label="`Remove artifact ${artifactChipLabel(id)}`"
                @click="form.inputs.splice(idx, 1)"
              >
                &times;
              </button>
            </li>
          </ul>
        </div>
        <small v-else class="hpc-field-help">No input artifacts selected.</small>
      </section>
    </div>

    <div class="hpc-form-actions mt-3">
      <button class="btn btn-primary btn-sm" @click="handleSubmit" :disabled="submitting">
        {{ submitting ? "Submitting..." : "Submit Job" }}
      </button>
    </div>
  </section>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from "vue";
import { submitJob, fetchArtifacts, fetchCapabilities } from "../composables/useHpcApi.js";

const emit = defineEmits(["submitted", "close"]);

const form = reactive({
  processor: "",
  profile: "",
  parametersJson: "",
  inputs: [],
});
const error = ref(null);
const submitting = ref(false);
const selectedArtifact = ref("");
const availableArtifacts = ref([]);
const capabilities = ref([]);

const processors = computed(() => [...new Set(capabilities.value.map((c) => c.processor))]);
const profiles = computed(() => {
  if (!form.processor) return [];
  return [
    ...new Set(
      capabilities.value.filter((c) => c.processor === form.processor).map((c) => c.profile),
    ),
  ];
});

watch(
  () => form.processor,
  () => {
    if (profiles.value.length && !profiles.value.includes(form.profile)) {
      form.profile = "";
    }
  },
);

function addArtifact() {
  if (selectedArtifact.value && !form.inputs.includes(selectedArtifact.value)) {
    form.inputs.push(selectedArtifact.value);
  }
  selectedArtifact.value = "";
}

function artifactChipLabel(id) {
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
    const payload = {
      processor: form.processor,
      profile: form.profile || undefined,
      parameters,
    };
    if (form.inputs.length) {
      payload.inputs = form.inputs;
    }
    await submitJob(payload);
    form.processor = "";
    form.profile = "";
    form.parametersJson = "";
    form.inputs = [];
    emit("submitted");
  } catch (e) {
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
