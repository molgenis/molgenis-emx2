<template>
    <div class="flex justify-center items-start">
      <div class="grow mb-2 self-center">
        <!-- TODO: Disabled html below is needed when we want to 
         implement a connection with the REMS store-->
        <span class="block font-bold">{{ resource.name }}</span>
        <!-- <div v-if="resource.datasets?.length">
          <div v-for="dataset in resource.datasets">
            <InputLabel
              :for="`${resource.id}-${dataset.name}`"
              class="group flex justify-start items-center relative"
            >
              <input
                type="checkbox"
                :id="`${resource.id}-${dataset.name}`"
                :name="resource.id"
                :value="dataset.name"
                v-model="modelValue"
                class="sr-only"
              />
              <InputCheckboxIcon
                :checked="modelValue!.includes(dataset.name)"
              />
              <span class="block" v-if="dataset.label">
                {{ dataset.label }}
              </span>
              <span class="block" v-else>
                {{ dataset.name }}
              </span>
            </InputLabel>
          </div>
        </div> -->
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
