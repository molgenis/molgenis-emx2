<script setup lang="ts">
import type { IHeaders, IFile } from "../../../types/cms";
import { ref } from "vue";
import ComponentActions from "./ComponentActions.vue";

const props = withDefaults(
  defineProps<IHeaders & { image?: IFile; isEditable?: boolean }>(),
  {
    enableFullScreenWidth: false,
    isEditable: false,
  }
);
const emit = defineEmits(["edit", "delete", "move"]);
const showMenu = ref<boolean>(true);
</script>

<template>
  <div
    class="w-full relative"
    @mouseenter="showMenu = true"
    @mouseleave="showMenu = false"
  >
    <header
      :id="id"
      class="group relative flex justify-center items-center h-72"
      :class="{
        'text-gray-100 bg-cover bg-center': image?.url,
        'text-title': !image?.url,
      }"
      :style="image?.url ? `background-image: url(${image?.url})` : ''"
    >
      <div
        class="m-auto mx-12.5 z-10"
        :class="{
          'w-pg-section': !enableFullScreenWidth,
          'w-full': enableFullScreenWidth,
          'text-center': titleIsCentered,
        }"
      >
        <h1 class="font-display text-heading-6xl">{{ title }}</h1>
        <p class="text-body-lg">{{ subtitle }}</p>
      </div>
      <div
        v-if="image?.url"
        class="absolute top-0 left-0 w-full h-full bg-black bg-opacity-60"
      />
    </header>
    <ComponentActions
      v-if="isEditable && showMenu"
      name="Header"
      :id="`${id}-toolbar`"
      :aria-controls="id"
      @edit="$emit('edit')"
      @delete="$emit('delete')"
      @move="$emit('move', $event)"
      class="right-2 top-2 !left-auto"
    />
  </div>
</template>
