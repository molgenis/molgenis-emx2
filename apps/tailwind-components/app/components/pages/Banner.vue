<script setup lang="ts">
import type { IHeaders } from "../../../types/cms";
import Button from "../Button.vue";

withDefaults(defineProps<IHeaders & { isEditable?: boolean }>(), {
  enableFullScreenWidth: false,
  isEditable: false,
});

const emit = defineEmits<{
  (e: "edit", value: boolean): void;
}>();
</script>

<template>
  <header
    :id="id"
    class="group relative flex justify-center items-center h-72"
    :class="{
      'text-gray-100 bg-cover bg-center': backgroundImage,
      'text-title': !backgroundImage,
    }"
    :style="backgroundImage ? `background-image: url(${backgroundImage})` : ''"
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
      v-if="backgroundImage"
      class="absolute top-0 left-0 w-full h-full bg-black bg-opacity-60"
    />
    <div v-if="isEditable" class="absolute bottom-5 right-5">
      <Button
        class="opacity-0 group-hover:opacity-100 group-focus:opacity-100"
        iconOnly
        icon="edit"
        label="Edit Header"
        type="secondary"
        size="small"
        aria-haspopup="true"
        @click="emit('edit', true)"
      />
    </div>
  </header>
</template>
