<template>
  <div>
    <ul class="tabs-header row justify-content-start">
      <li
        v-for="title in tabTitles"
        :key="title"
        class="tabs-item"
        :class="{ selected: selectedTitle === title }"
        @click="setTitle(title)"
      >
        {{ title }}
      </li>
      <li class="filler-line col"></li>
    </ul>

    <slot class="tab-content" />
  </div>
</template>

<script setup lang="ts">
import { useSlots, ref, provide } from "vue";
const slots = useSlots();
const tabTitles = ref(slots.default().map((tab) => tab.props?.title));
const selectedTitle = ref(tabTitles.value[0]);

provide("selectedTitle", selectedTitle);

function setTitle(newTitle: string) {
  selectedTitle.value = newTitle;
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
    <Tabs>
      <Tab title="First tab">This is the first tab</Tab>
      <Tab title="Second tab">This is the content of the second tab</Tab>
    </Tabs>
  </template>
</docs>
