<template>
  <SideModal
    :show="show"
    :slideInRight="true"
    :fullScreen="false"
    :includeFooter="true"
    buttonAlignment="left"
    @close="$emit('close')"
  >
    <ContentBlockModal title="Collections">
      <template
        v-if="
          datasetStore.datasets && Object.keys(datasetStore.datasets).length > 0
        "
      >
        <p class="mb-2">Review selected collections and linked datasets</p>
        <StoreModalResourceList />
      </template>
      <p v-else>Cart is empty</p>
    </ContentBlockModal>
    <template #footer>
      <a
        :href="datasetStoreUrl"
        target="_blank"
        class="flex items-center border rounded-input h-14 px-7.5 text-heading-xl tracking-widest uppercase font-display bg-button-primary text-button-primary border-button-primary hover:bg-button-primary-hover hover:text-button-primary-hover hover:border-button-primary-hover"
      >
        <span>Request from {{ getSendToText() }}</span>
        <BaseIcon name="external-link" :width="24" />
      </a>
    </template>
  </SideModal>
</template>

<script lang="ts" setup>
import { useDatasetStore } from "../../stores/useDatasetStore";
import SideModal from "../../../../tailwind-components/app/components/SideModal.vue";
import ContentBlockModal from "../../../../tailwind-components/app/components/content/ContentBlockModal.vue";
import StoreModalResourceList from "./ModalResourceList.vue";
import BaseIcon from "../../../../tailwind-components/app/components/BaseIcon.vue";

const datasetStore = useDatasetStore();

const datasetStoreUrl = await datasetStore.getDatasetStoreUrl();

withDefaults(
  defineProps<{
    show: boolean;
  }>(),
  {
    show: false,
  }
);

defineEmits<{
  (e: "close"): void;
}>();

function getSendToText() {
  const version = datasetStore.storeVersion;
  switch (version) {
    case "GDI":
      return "GDI";
    case "v3":
      return "Negotiator";
    default:
      return "Unknown data store";
  }
}
</script>
