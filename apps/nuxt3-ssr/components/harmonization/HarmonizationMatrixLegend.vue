<script setup lang="ts">
import type { HarmonizationIconSize } from "../../interfaces/types";

const props = withDefaults(
  defineProps<{
    size?: HarmonizationIconSize;
  }>(),
  { size: "large" }
);

let modalIsOpen = ref<boolean>(false);

function openModal() {
  modalIsOpen.value = true;
}

function closeModal() {
  modalIsOpen.value = false;
}
</script>
<template>
  <div class="flex flex-row justify-end items-center h-16 mr-[2em] gap-5">
    <ul
      class="flex justify-end items-center gap-3 mr-3 list-none [&_li]:flex [&_li]:items-center [&_li]:gap-2"
    >
      <li>
        <HarmonizationStatusIcon :size="size" status="complete" />
        available
      </li>
    </ul>
    <div class="flex justify-center">
      <button
        id="harmonization-matrix-legend-show-model"
        class="p-0 m-0"
        title="About statuses"
        @click="openModal"
      >
        <BaseIcon name="Info" class="text-blue-500" />
        <span class="sr-only">about statuses</span>
      </button>
    </div>
  </div>
  <SideModal
    :show="modalIsOpen"
    :slideInRight="true"
    :fullScreen="false"
    type="light"
    :includeFooter="false"
    @close="closeModal"
  >
    <ContentBlockModal
      title="About statuses"
      description="The following statuses are used to define the availability of variables in a cohort."
    >
      <DefinitionList>
        <DefinitionListTerm>Available</DefinitionListTerm>
        <DefinitionListDefinition>
          cohort has data available for the variable
        </DefinitionListDefinition>
      </DefinitionList>
    </ContentBlockModal>
  </SideModal>
</template>
