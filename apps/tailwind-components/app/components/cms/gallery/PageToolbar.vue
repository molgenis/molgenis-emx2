<script setup lang="ts">
import { computed } from "vue";
import { useRoute } from "vue-router";

import {
  setCmsEditorUrl,
  setCmsViewUrl,
  setCmsPageType,
} from "../../../utils/cms";

const props = withDefaults(
  defineProps<{
    isEditable: boolean;
    pageType: string;
  }>(),
  {
    isEditable: false,
  }
);

const route = useRoute();
const schema = computed<string>(() => {
  const currentSchema = Array.isArray(route.params.schema)
    ? route.params.schema[0]
    : route.params.schema ?? "";
  return currentSchema as string;
});

const page = computed<string>(() => route.path);

const inEditMode = computed<boolean>(() => {
  const hasEditorPath =
    page.value.endsWith("/configure") || page.value.endsWith("/editor");
  return props.isEditable && hasEditorPath;
});

const url = computed<string>(() => {
  const targetPage = route.params.page as string;
  if (inEditMode.value) {
    return setCmsViewUrl(schema.value, targetPage);
  } else {
    return setCmsEditorUrl(schema.value, props.pageType, targetPage);
  }
});

const urlLabel = computed<string>(() => {
  return inEditMode.value ? "Preview page" : "Edit page";
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
        <BaseIcon :name="inEditMode ? 'Visible' : 'Edit'" />
        <span class="sr-only">{{ urlLabel }}</span>
      </NuxtLink>
    </div>
  </div>
</template>
