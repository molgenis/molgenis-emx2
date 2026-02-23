<template>
  <span class="badge" :class="badgeClass">{{ status }}</span>
</template>

<script setup>
import { computed } from "vue";

const props = defineProps({
  status: { type: String, required: true },
});

const badgeClass = computed(() => {
  const map = {
    // Job statuses
    PENDING: "bg-warning text-dark",
    CLAIMED: "bg-info text-dark",
    SUBMITTED: "bg-primary",
    STARTED: "bg-primary",
    COMPLETED: "bg-success",
    FAILED: "bg-danger",
    CANCELLED: "bg-secondary",
    // Artifact statuses
    CREATED: "bg-warning text-dark",
    UPLOADING: "bg-info text-dark",
    REGISTERED: "bg-info text-dark",
    COMMITTED: "bg-success",
  };
  return map[props.status] || "bg-secondary";
});
</script>
