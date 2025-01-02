<template>
  <div>
    <PageSection
      id="genturis-section-welcome"
      aria-labelledby="genturis-section-welcome-title"
      :verticalPadding="2"
      width="large"
    >
      <h2 id="genturis-section-welcome-title">
        {{ localContent.title }}
      </h2>
      <div v-html="localContent.html"></div>
    </PageSection>
    <EditBlock v-if="editMode" type="Section">
      <InputLabel for="input-title">
        Title
      </InputLabel>
      <InputString
        id="input-title"
        :modelValue="localContent.title"
        @update:modelValue="save('title', $event)"
      />
      <InputLabel for="html-editor">
        HTML
      </InputLabel>
      <QuillEditor
        id="html-editor"
        :content="localContent.html"
        @update:content="save('html', $event)"
        toolbar="full"
        class="bg-white"
        contentType="html"
      />
    </EditBlock>
  </div>
</template>

<script setup lang="ts">
// @ts-ignore
import { PageSection } from "molgenis-viz";
import { QuillEditor } from "@vueup/vue-quill";
import "@vueup/vue-quill/dist/vue-quill.snow.css";

import EditBlock from "../EditBlock.vue";
import { ref, watch } from "vue";
let props = withDefaults(
  defineProps<{
    content?: { title: string; html: string };
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
