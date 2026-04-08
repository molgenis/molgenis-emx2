<template>
  <span
    class="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full text-body-xs font-medium"
    :class="pillClass"
  >
    <span
      class="w-1.5 h-1.5 rounded-full shrink-0"
      :class="[dotClass, isActive ? 'animate-pulse' : '']"
      aria-hidden="true"
    ></span>
    {{ label }}
  </span>
</template>

<script setup lang="ts">
import { computed } from "vue";

const props = defineProps<{
  status: string;
}>();

const label = computed(() => String(props.status || "").replaceAll("_", " "));

type StatusGroup = "waiting" | "progress" | "success" | "error" | "neutral";

const GROUP_MAP: Record<string, StatusGroup> = {
  PENDING: "waiting",
  CREATED: "waiting",
  CLAIMED: "progress",
  SUBMITTED: "progress",
  STARTED: "progress",
  UPLOADING: "progress",
  REGISTERED: "progress",
  COMPLETED: "success",
  COMMITTED: "success",
  FAILED: "error",
  CANCELLED: "neutral",
};

const TERMINAL: Set<string> = new Set([
  "COMPLETED",
  "FAILED",
  "CANCELLED",
  "COMMITTED",
]);

const group = computed((): StatusGroup => GROUP_MAP[props.status] || "neutral");

const pillClass = computed(() => {
  const map: Record<StatusGroup, string> = {
    waiting: "bg-yellow-200 text-yellow-800",
    progress: "bg-blue-100 text-blue-700",
    success: "hpc-badge-success text-green-800",
    error: "hpc-badge-error text-red-700",
    neutral: "bg-gray-100 text-gray-600",
  };
  return map[group.value];
});

const dotClass = computed(() => {
  const map: Record<StatusGroup, string> = {
    waiting: "bg-yellow-800",
    progress: "bg-blue-500",
    success: "bg-green-800",
    error: "bg-red-500",
    neutral: "bg-gray-400",
  };
  return map[group.value];
});

const isActive = computed(() => !TERMINAL.has(props.status));
</script>

<style scoped>
.hpc-badge-success {
  background-color: rgb(114 246 178 / 0.2);
}
.hpc-badge-error {
  background-color: rgb(225 79 98 / 0.15);
}
</style>
