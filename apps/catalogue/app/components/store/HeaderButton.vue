<template>
  <Button label="Request " @click="$emit('click')">
    <span class="inline-block w-6 h-6 bg-white" style="border-radius: 100%">
      <span class="font-bold body-xs/1">
        {{ numberOfItemsInStore }}
      </span>
    </span>
  </Button>
</template>

<script lang="ts" setup>
import { useDatasetStore } from "#imports";
import { ref, watch, computed } from "vue";
import Button from "../../../../tailwind-components/app/components/Button.vue";

const datasetStore = useDatasetStore();

defineEmits<{ (e: "click"): void }>();

const storeHasDatasets = ref<boolean>(false);

watch([datasetStore.datasets], () => {
  storeHasDatasets.value = !!Object.keys(datasetStore.datasets).length;
});

const numberOfItemsInStore = computed<number | string>(() => {
  if (storeHasDatasets.value) {
    const count = Object.keys(datasetStore.datasets).length;
    if (count > 99) {
      return "99+";
    } else {
      return count;
    }
  }
  return 0;
});
</script>
