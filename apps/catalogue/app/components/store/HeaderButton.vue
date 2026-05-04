<template>
  <Button label="Request " @click="$emit('click')">
    <span class="inline-block w-6 h-6 bg-white" style="border-radius: 100%">
      <span class="body-xs/1">
        {{ numberOfItemsInStore }}
      </span>
    </span>
  </Button>
</template>

<script lang="ts" setup>
import { computed } from "vue";
import Button from "../../../../tailwind-components/app/components/Button.vue";
import { useDatasetStore } from "../../stores/useDatasetStore";

const datasetStore = useDatasetStore();

defineEmits<{ (e: "click"): void }>();

const numberOfItemsInStore = computed<number | string>(() => {
  const count = Object.keys(datasetStore.datasets).length;
  if (count > 99) {
    return "99+";
  } else {
    return count;
  }
});
</script>
