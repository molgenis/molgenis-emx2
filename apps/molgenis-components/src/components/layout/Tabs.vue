<template>
  <div>
    <ul class="tabs-header row justify-content-start">
      <li
        v-for="tabId in tabIds"
        :key="tabId"
        class="tabs-item"
        :class="{ selected: selectedTab === tabId }"
        @click="setTab(tabId)"
      >
        <slot :name="`${tabId}-header`" />
      </li>
      <li class="filler-line col"></li>
    </ul>
    <div
      v-for="tabId in tabIds"
      v-show="tabId === selectedTab"
      class="tabs-content"
    >
      <slot :name="`${tabId}-body`" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";

const { tabIds } = defineProps<{ tabIds: string[] }>();

const selectedTab = ref(tabIds[0]);

function setTab(newId: string) {
  selectedTab.value = newId;
}
</script>

<style scoped>
.filler-line {
  border-bottom: 1px solid #dee2e6;
}
.tabs-header {
  list-style: none;
  padding: 0;
  margin: 0;
  justify-content: space-between;
}

.tabs-item {
  color: #ec6707;
  padding: 5px;
  cursor: pointer;
  font-size: 1.25rem;
  border-bottom: 1px solid #dee2e6;
}

.tabs-item.selected {
  border: 1px solid #dee2e6;
  border-bottom: none;
}

.tabs-content {
  min-height: 300px;
  display: grid;
  border-radius: 0 0 5px 5px;
  padding: 10px;
}
</style>

<docs>
<template>
  <Tabs :tabIds="['first', 'second']">
    <template #first-header>First</template>
    <template #first-body> This is the first tab </template>

    <template #second-header>Second</template>
    <template #second-body>This is the content of the second tab</template>
  </Tabs>
</template>
</docs>
