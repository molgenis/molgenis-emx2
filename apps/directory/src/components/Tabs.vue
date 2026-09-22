<template>
  <ul class="nav nav-tabs mb-2 mt-2 pl-2">
    <li v-for="tab in Object.keys(tabs)" class="nav-item">
      <button
        type="button"
        class="btn btn-link nav-link shadow-none"
        :disabled="tabs[tab].disabled"
        :class="{ active: tabs[tab].active }"
        @click="() => $emit('update:activeTab', tab)"
      >
        {{ tabs[tab].label ?? tabs[tab].id }}
      </button>
    </li>
  </ul>
</template>

<script setup lang="ts">
defineProps<{
  tabs: Record<
    string,
    { id: string; label: string; active: boolean; disabled: boolean }
  >;
}>();

defineEmits(["update:activeTab"]);
</script>
