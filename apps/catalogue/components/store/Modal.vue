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
          datasetStore.datasets.value &&
          Object.keys(datasetStore.datasets.value).length > 0
        "
      >
        <p class="mb-2">Review selected collections and linked datasets</p>
        <StoreModalResourceList />
      </template>
      <p v-else>Cart is empty</p>
    </ContentBlockModal>
    <template #footer>
      <a
        href="https://catalogue.portal.dev.gdi.lu/organization/umcg"
        class="flex items-center border rounded-input h-14 px-7.5 text-heading-xl tracking-widest uppercase font-display bg-button-primary text-button-primary border-button-primary hover:bg-button-primary-hover hover:text-button-primary-hover hover:border-button-primary-hover"
      >
        <span>Request from GDI</span>
        <BaseIcon name="external-link" :width="24" />
      </a>
    </template>
  </SideModal>
</template>

<script lang="ts" setup>
import { useDatasetStore } from "~/stores/useDatasetStore";

const datasetStore = useDatasetStore();

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
</script>
