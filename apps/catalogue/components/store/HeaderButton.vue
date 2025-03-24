<template>
  <div class="relative">
    <HeaderButton
      label="Selected items"
      icon="shopping-cart"
      class="xl:text-blue-500"
      @click="$emit('click')"
    />
    <div
      v-if="storeHasDatasets"
      class="absolute flex items-center justify-center top-0 left-0 inline-block text-center w-6 h-6 bg-blue-200 -translate-y-2 translate-x-4"
      style="border-radius: 100%"
    >
      <span class="text-blue-800 font-bold body-xs/1">
        {{ numberOfItemsInStore }}
      </span>
    </div>
  </div>
</template>

<script lang="ts" setup>
const datasetStore = useDatasetStore();

defineEmits<{ (e: "click"): void }>();

const storeHasDatasets = ref<boolean>(false);

watch([datasetStore.datasets], () => {
  storeHasDatasets.value =
    datasetStore.datasets.value &&
    Object.keys(datasetStore.datasets.value).length > 0;
});

const numberOfItemsInStore = computed<number | string>(() => {
  if (storeHasDatasets.value) {
    const count = Object.keys(datasetStore.datasets.value).length;
    if (count > 99) {
      return "99+";
    } else {
      return count;
    }
  }
  return 0;
});
</script>
