<template>
  <div class="flex justify-center items-start">
    <div class="grow mb-2 self-center">
      <span class="block font-bold">{{ resource.name }}</span>
    </div>
    <div>
      <IconButton
        icon="trash"
        @click="() => datasetStore.removeFromCart(resource.id)"
        class="text-red-500 mb-2"
        label="remove collection from cart"
      />
    </div>
  </div>
</template>

<script lang="ts" setup>
import { useDatasetStore } from "../../stores/useDatasetStore";
import { onMounted, ref } from "vue";
import IconButton from "../../../../tailwind-components/app/components/button/IconButton.vue";
import type { IDatasets, IResources } from "../../../interfaces/catalogue";

const datasetStore = useDatasetStore();
const modelValue = ref<string[]>([]);

const props = defineProps<{
  resource: IResources;
}>();

onMounted(() => {
  if (props.resource.datasets?.length) {
    modelValue.value = props.resource.datasets.map(
      (row: IDatasets) => row.name
    );
  }
});
</script>
