<template>
  <div>
    <PageHeader
      :title="localContent.title"
      :subtitle="localContent.subtitle"
      imageSrc="https://emx2.dev.molgenis.org/apps/ern-genturis/img/genturis-carousel.jpg"
      titlePositionX="center"
      titlePositionY="center"
    />
    <EditBlock v-if="editMode" type="Header">
      <InputString
        id="title"
        ß
        :modelValue="localContent.title"
        @update:modelValue="save('title', $event)"
      />
      <InputString
        id="subtitle"
        :modelValue="localContent.subtitle"
        @update:modelValue="save('subtitle', $event)"
      />
    </EditBlock>
  </div>
</template>

<script setup lang="ts">
// @ts-ignore
import { PageHeader } from "molgenis-viz";
// @ts-ignore
import { InputString } from "molgenis-components";
import EditBlock from "../EditBlock.vue";

import { ref, watch } from "vue";

let props = withDefaults(
  defineProps<{
    content?: { title?: string; subtitle?: string };
    editMode?: boolean;
  }>(),
  {
    editMode: false,
  }
);

const emit = defineEmits();

let localContent = ref(props.content);

function save(key: string, value: string) {
  localContent.value[key] = value;
  emit("save", localContent.value);
}

watch(
  () => props.content,
  (newValue) => {
    localContent.value = newValue;
  }
);
</script>
