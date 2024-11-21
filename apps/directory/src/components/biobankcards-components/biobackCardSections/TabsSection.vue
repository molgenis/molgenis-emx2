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
  <div class="tabs">
    <button
      v-for="tab in tabs"
      class="tab"
      :class="{ active: activeTab === tab }"
      @click="() => $emit('update:activeTab', tab)"
    >
      {{ tab }}
    </button>
  </div>
</template>

<style scoped>
.tabs {
  display: flex;
  justify-content: space-between;
  margin: 1rem;
}

.tab {
  background-color: white;
  border: none;
  cursor: pointer;
}

.tab:hover {
  cursor: pointer;
  text-decoration: underline;
}

.tab.active {
  color: #ec6707;
  text-decoration: none;
  cursor: default;
}
</style>
