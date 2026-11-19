<script setup lang="ts">
import { computed } from "vue";

import {
  setCmsEditorUrl,
  setCmsViewUrl,
  setCmsPageType,
} from "../../../utils/cms";

const props = withDefaults(
  defineProps<{
    isEditable: boolean;
    pageType: string;
    route: Record<string, any>;
    schema: string;
  }>(),
  {
    isEditable: false,
  }
);

const page = computed<string>(() => {
  return props.route.params.page as string;
});

const path = computed<string>(() => {
  return props.route.path;
});

const inEditMode = computed<boolean>(() => {
  return path.value.endsWith("configure") || path.value.endsWith("editor");
});

const url = computed<string>(() => {
  if (props.isEditable && inEditMode.value) {
    return setCmsViewUrl(props.schema, page.value);
  } else {
    return setCmsEditorUrl(props.schema, props.pageType, page.value);
  }
});

const urlLabel = computed<string>(() => {
  return inEditMode.value ? "Preview page" : "Edit page";
});

const urlIcon = computed<string>(() => {
  return inEditMode.value ? "Visible" : "Edit";
});
</script>

<template>
  <div class="fixed bottom-10 right-10">
    <div class="flex justify-end flex-col gap-2.5">
      <NuxtLink
        :to="url"
        v-tooltip.top="urlLabel"
        class="flex justify-center items-center rounded-full bg-button-primary text-button-primary p-2.5 cursor-pointer"
      >
        <BaseIcon :name="urlIcon" />
        <span class="sr-only">{{ urlLabel }}</span>
      </NuxtLink>
    </div>
  </div>
</template>
