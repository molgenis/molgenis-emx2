<template>
  <div>
    <ul class="tabs-header row">
      <li
        v-for="title in tabTitles"
        :key="title"
        class="tabs-item col"
        :class="{ selected: selectedTitle === title }"
        @click="selectedTitle = title"
      >
        {{ title }}
      </li>
    </ul>

    <slot />
  </div>
</template>

<script setup lang="ts">
import { useSlots, ref, provide } from "vue";

const slots = useSlots();
const tabTitles = ref(slots.default().map((tab) => tab.props?.title));
const selectedTitle = ref(tabTitles.value[0]);

provide("selectedTitle", selectedTitle);
</script>

<style>
.tabs-header {
  list-style: none;
  padding: 0;
  margin: 0;
  justify-content: space-between;
  gap: 5px;
}

.tabs-item {
  color: #ec6707;
  padding: 5px 0;
  cursor: pointer;
  font-size: 1.25rem;
  border: 1px solid black;
}

.tabs-item.selected {
  border-bottom: none;
}

.tabs-content {
  min-height: 300px;
  display: grid;
  border-radius: 0 0 5px 5px;
  padding: 10px;
}
</style>
