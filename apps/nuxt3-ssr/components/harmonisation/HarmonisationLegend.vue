<script setup lang="ts">
import type { HarmonisationIconSize } from "../../interfaces/types";

const props = withDefaults(
  defineProps<{
    size?: HarmonisationIconSize;
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
        <HarmonisationStatusIcon :size="size" status="complete" />
        Completed
      </li>
      <li>
        <HarmonisationStatusIcon :size="size" status="partial" />
        Partial
      </li>
      <li>
        <HarmonisationStatusIcon :size="size" status="unmapped" />
        No Data
      </li>
    </ul>
    <div class="flex justify-center">
      <button
        id="about-status-show-model"
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
      description="The following statuses are used to define the progress of harmonisation between variables."
    >
      <dl
        class="grid grid-cols-3 [&_dt]:col-span-1 [&_dd]:col-span-2 [&_*]:p-1 [&_*]:border-b-[1px] [&_*]:border-b-gray-200"
      >
        <dt>Complete</dt>
        <dd>Harmonisation is complete.</dd>
        <dt>Partial</dt>
        <dd>
          Harmonisation is incomplete. It may be the cases that this process is
          ongoing or cannot progress further.
        </dd>
        <dt>No Data</dt>
        <dd>
          Harmonisation is unknown. Either information is not available or it is
          not possible to harmonise these variables.
        </dd>
      </dl>
    </ContentBlockModal>
  </SideModal>
</template>
