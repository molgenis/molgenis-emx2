<script setup lang="ts">
import { ref, computed } from "vue";
import { useSession } from "../../../composables/useSession";
import { setCmsEditorUrl, setCmsViewUrl } from "../../../utils/cms";
import SaveState from "../SaveState.vue";

const props = defineProps<{
  pageType: string;
  route: Record<string, any>;
  schema: string;
  saving: boolean;
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

const showToolbar = ref<boolean>(true);
</script>

<template>
  <div class="fixed bottom-7.5 right-0">
    <div class="flex flex-row justify-end pointer-events-none m-6 text-title"><SaveState :saving='saving'/></div> 
    <div
      v-if="enableEditing"
      class="flex flex-row items-center gap-2.5 p-2 overflow-x-hidden rounded-tl-base rounded-bl-base transition-[transform] origin-center duration-default motion-reduce:transition-none border-theme border-color-theme box-border bg-navigation shadow-lg"
      :class="{
        'translate-x-[72.5%]': !showToolbar,
        'translate-x-[0%]': showToolbar,
      }"
    >
      <button
        class="px-2.5 text-title"
        @click="showToolbar = !showToolbar"
        :aria-expanded="showToolbar"
        aria-controls="page-toolbar-menu"
      >
        <BaseIcon
          :name="showToolbar ? 'double-arrow-right' : 'double-arrow-left'"
        />
        <span class="sr-only">
          {{ showToolbar ? "hide" : "show" }} toolbar
        </span>
      </button>
      <div
        id="page-toolbar-menu"
        :aria-hidden="!showToolbar"
        class="px-3.5 border-l-theme border-color-theme"
      >
        <nav aria-labelledby="page-toolbar-label">
          <span id="page-toolbar-label" class="sr-only">switch page mode</span>
          <ul class="flex justify-end flex-row gap-2.5 text-title">
            <li>
              <NuxtLink
                :to="url"
                v-tooltip.top="urlLabel"
                class="flex justify-center items-center rounded-full hover:bg-button-primary hover:text-button-primary h-10 w-10 p-2.5 cursor-pointer"
              >
                <BaseIcon :name="urlIcon" :width="18" />
                <span class="sr-only">{{ urlLabel }}</span>
              </NuxtLink>
            </li>
            <li>
              <NuxtLink
                :to="`/${schema}/pages`"
                v-tooltip.top="`Page Gallery`"
                class="flex justify-center items-center rounded-full hover:bg-button-primary hover:text-button-primary h-10 w-10 p-2.5 cursor-pointer"
              >
                <BaseIcon name="Dataset" :width="18" />
                <span class="sr-only">go to the page gallery</span>
              </NuxtLink>
            </li>
          </ul>
        </nav>
      </div>
    </div>
  </div>
</template>
