<template>
  <span class="hpc-status-pill" :class="statusClass">
    <span class="hpc-status-dot" aria-hidden="true"></span>
    <span>{{ label }}</span>
  </span>
</template>

<script setup>
import { computed } from "vue";

const props = defineProps({
  status: { type: String, required: true },
});

const label = computed(() => String(props.status || "").replaceAll("_", " "));

const statusClass = computed(() => {
  const map = {
    // Job statuses
    PENDING: "is-warn",
    CLAIMED: "is-info",
    SUBMITTED: "is-primary",
    STARTED: "is-primary",
    COMPLETED: "is-success",
    FAILED: "is-danger",
    CANCELLED: "is-muted",
    // Artifact statuses
    CREATED: "is-warn",
    UPLOADING: "is-info",
    REGISTERED: "is-info",
    COMMITTED: "is-success",
  };
  return map[props.status] || "is-muted";
});
</script>
