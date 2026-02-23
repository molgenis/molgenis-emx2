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
    PENDING: "bg-warning text-dark",
    CLAIMED: "bg-info text-dark",
    SUBMITTED: "bg-primary",
    STARTED: "bg-primary",
    COMPLETED: "bg-success",
    FAILED: "bg-danger",
    CANCELLED: "bg-secondary",
  };
  return map[props.status] || "bg-secondary";
});
</script>
