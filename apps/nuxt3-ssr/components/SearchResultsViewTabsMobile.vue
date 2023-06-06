<script setup>
import { computed } from "vue";
import BaseIcon from "./BaseIcon.vue";

const { activeName } = defineProps({
  activeName: {
    type: String,
  },
});

const emit = defineEmits(["update:activeName"]);

function toggleMode(detailed) {
  if (detailed) {
    emit("update:activeName", "compact");
  } else {
    emit("update:activeName", "detailed");
  }
}
</script>
<template>
  <div class="flex justify-between mt-5">
    <SideModal :fullScreen="false">
      <slot></slot>

      <template #button>
        <Button
          type="primary"
          size="small"
          label="filters"
          icon="filter"
          iconPosition="left"></Button>
      </template>
    </SideModal>

    <Button
      v-if="activeName === 'detailed'"
      type="secondary"
      size="small"
      label="view"
      icon="view-normal"
      iconPosition="left"
      @click="toggleMode(true)"></Button>
    <Button
      v-else
      type="secondary"
      size="small"
      label="view"
      icon="view-compact"
      iconPosition="left"
      @click="toggleMode(false)"></Button>
  </div>
</template>
