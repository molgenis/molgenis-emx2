<template>
  <div class="flex justify-center items-start">
    <div class="grow mb-2 self-center">
      <span class="block font-bold">{{ resource.name }}</span>
    </div>
    <div>
      <IconButton
        icon="trash"
        @click="() => datasetStore.removeFromCart(resource.id)"
        class="text-button-remove mb-2"
        label="remove collection from cart"
      />
    </div>
  </div>
</template>

<script lang="ts" setup>
import { useDatasetStore } from "../../stores/useDatasetStore";
import { onMounted, ref } from "vue";
import IconButton from "../../../../tailwind-components/app/components/button/IconButton.vue";
import type { ITables, IResources } from "../../../interfaces/catalogue";

const datasetStore = useDatasetStore();
const modelValue = ref<string[]>([]);

const props = defineProps<{
  resource: IResources;
}>();

onMounted(() => {
  if (props.resource.tables?.length) {
    modelValue.value = props.resource.tables.map((row: ITables) => row.name);
  }
});
</script>
