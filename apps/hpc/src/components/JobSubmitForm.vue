<template>
  <div class="card mb-3">
    <div class="card-header d-flex justify-content-between align-items-center">
      <strong>Submit New Job</strong>
      <button class="btn btn-sm btn-outline-secondary" @click="$emit('close')">Cancel</button>
    </div>
    <div class="card-body">
      <div v-if="error" class="alert alert-danger">{{ error }}</div>
      <div class="mb-2">
        <label class="form-label">Processor *</label>
        <input v-model="form.processor" class="form-control form-control-sm" placeholder="e.g. text-embedding" />
      </div>
      <div class="mb-2">
        <label class="form-label">Profile</label>
        <input v-model="form.profile" class="form-control form-control-sm" placeholder="e.g. gpu-medium" />
      </div>
      <div class="mb-2">
        <label class="form-label">Parameters (JSON)</label>
        <textarea v-model="form.parametersJson" class="form-control form-control-sm" rows="3"
          placeholder='{"key": "value"}'></textarea>
      </div>
      <div class="mb-3">
        <label class="form-label">Input Artifacts</label>
        <div class="d-flex gap-2 mb-1">
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
        <div v-if="form.inputs.length">
          <span
            v-for="(id, idx) in form.inputs"
            :key="id"
            class="badge bg-light text-dark border me-1"
          >
            {{ id?.substring(0, 8) }}
            <button
              type="button"
              class="btn-close btn-close-sm ms-1"
              style="font-size: 0.6em"
              @click="form.inputs.splice(idx, 1)"
            ></button>
          </span>
        </div>
      </div>
      <button class="btn btn-primary btn-sm" @click="handleSubmit" :disabled="submitting">
        {{ submitting ? "Submitting..." : "Submit Job" }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from "vue";
import { submitJob, fetchArtifacts } from "../composables/useHpcApi.js";

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

function addArtifact() {
  if (selectedArtifact.value && !form.inputs.includes(selectedArtifact.value)) {
    form.inputs.push(selectedArtifact.value);
  }
  selectedArtifact.value = "";
}

async function loadArtifacts() {
  try {
    const result = await fetchArtifacts({ status: "COMMITTED", limit: 100 });
    availableArtifacts.value = result.items;
  } catch {
    // silently ignore — artifact selection is optional
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

onMounted(loadArtifacts);
</script>
