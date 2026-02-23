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
      <button class="btn btn-primary btn-sm" @click="handleSubmit" :disabled="submitting">
        {{ submitting ? "Submitting..." : "Submit Job" }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from "vue";
import { submitJob } from "../composables/useHpcApi.js";

const emit = defineEmits(["submitted", "close"]);

const form = reactive({
  processor: "",
  profile: "",
  parametersJson: "",
});
const error = ref(null);
const submitting = ref(false);

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
    await submitJob({
      processor: form.processor,
      profile: form.profile || undefined,
      parameters,
    });
    form.processor = "";
    form.profile = "";
    form.parametersJson = "";
    emit("submitted");
  } catch (e) {
    error.value = e.message;
  } finally {
    submitting.value = false;
  }
}
</script>
