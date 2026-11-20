<script setup lang="ts">
import { computed } from "vue";
import { useSession } from "../../../composables/useSession";
import { setCmsEditorUrl, setCmsViewUrl } from "../../../utils/cms";

const props = defineProps<{
  pageType: string;
  route: Record<string, any>;
  schema: string;
}>();

const { isAdmin, session } = await useSession(props.schema);
const enableEditing = computed(() => {
  return (
    session.value?.roles?.[props.schema]?.includes("Manager") || isAdmin.value
  );
});

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
  if (enableEditing.value && inEditMode.value) {
    return setCmsViewUrl(props.schema, page.value);
  } else {
    return setCmsEditorUrl(props.schema, props.pageType, page.value);
  }
});

const urlLabel = computed<string>(() => {
  return inEditMode.value ? "Preview page" : "Edit page";
});

const urlIcon = computed<string>(() => {
  return inEditMode.value ? "Image" : "Edit";
});
</script>

<template>
  <div class="fixed bottom-7.5 right-7.5">
    <nav class="flex justify-end flex-col gap-2.5">
      <NuxtLink
        v-if="enableEditing"
        :to="url"
        v-tooltip.top="urlLabel"
        class="flex justify-center items-center rounded-full bg-button-primary text-button-primary p-2.5 cursor-pointer"
      >
        <BaseIcon :name="urlIcon" />
        <span class="sr-only">{{ urlLabel }}</span>
      </NuxtLink>
    </nav>
  </div>
</template>
