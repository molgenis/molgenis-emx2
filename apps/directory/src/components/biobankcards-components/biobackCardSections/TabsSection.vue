<script setup lang="ts">
withDefaults(
  defineProps<{
    activeTab: string;
    tabs: string[];
  }>(),
  {
    activeTab: "Collections",
  }
);

defineEmits(["update:activeTab"]);
</script>
<template>
  <ul class="nav nav-tabs mb-2 mt-2 pl-2">
    <li v-for="tab in tabs" class="nav-item">
      <button
        type="button"
        class="btn btn-link nav-link shadow-none"
        :class="{ active: activeTab === tab }"
        @click="() => $emit('update:activeTab', tab)"
      >
        {{ tab }}
      </button>
    </li>
  </ul>
</template>
