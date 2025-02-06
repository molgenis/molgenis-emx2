<template>
  <div>
    <PageHeader
      :title="localContent.title"
      :subtitle="localContent.subtitle"
      imageSrc="https://emx2.dev.molgenis.org/apps/ern-genturis/img/genturis-carousel.jpg"
      titlePositionX="center"
      titlePositionY="center"
    />
    <EditBlock v-if="editMode" type="Header" @action="$emit('action', $event)">
      <InputLabel for="header-title-input-string">
        Title
      </InputLabel>
      <InputString
        id="header-title-input-string"
        :modelValue="localContent.title"
        @update:modelValue="save('title', $event)"
      />
      <InputLabel for="header-subtitle-input-string">
        Subtitle
      </InputLabel>
      <InputString
        id="header-subtitle-input-string"
        :modelValue="localContent.subtitle"
        @update:modelValue="save('subtitle', $event)"
      />
    </EditBlock>
  </div>
</template>

<script setup lang="ts">
// @ts-ignore
import { PageHeader } from "molgenis-viz";
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

const emit = defineEmits(["save", "action"]);

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
