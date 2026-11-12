<script setup lang="ts">
import type { IHeaders, IFile } from "../../../../types/cms";
const props = defineProps<
  IHeaders & { image?: IFile } & { isEditable: boolean }
>();
const emit = defineEmits(["updateTitle", "updateSubtitle"]);
</script>

<template>
  <header
    :id="id"
    class="group relative flex justify-center items-center"
    :class="{
      'text-gray-100 bg-cover bg-center': image?.url,
      'text-title': !image?.url,
      'h-72': pageHeaderHeight?.name == 'default' || !pageHeaderHeight?.name,
      'h-48': pageHeaderHeight?.name === 'small',
      'h-96': pageHeaderHeight?.name == 'large',
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
      <h1
        class="font-display text-heading-6xl"
        :contenteditable="props.isEditable"
        @input="
          props.isEditable &&
            emit('updateTitle', ($event.target as HTMLElement)?.innerHTML ?? '')
        "
      >
        {{ title }}
      </h1>
      <p
        class="text-body-lg"
        :contenteditable="props.isEditable"
        @input="
          props.isEditable &&
            emit(
              'updateSubtitle',
              ($event.target as HTMLElement)?.innerHTML ?? ''
            )
        "
      >
        {{ subtitle }}
      </p>
    </div>
    <div
      v-if="image?.url"
      class="absolute top-0 left-0 w-full h-full bg-black bg-opacity-60"
    />
  </header>
</template>
