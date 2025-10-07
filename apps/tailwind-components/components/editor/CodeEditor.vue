<script lang="ts" setup>
import { ref, useTemplateRef, onMounted } from "vue";

const props = withDefaults(
  defineProps<{
    lang: "html" | "css" | "javascript";
    modelValue?: string;
  }>(),
  {
    lang: "html",
  }
);

const emits = defineEmits<{
  (e: "update:modelValue", value: string): void;
}>();

const code = ref<string>(props.modelValue as string);
const editor = useTemplateRef("editor");

function formatEditor() {
  editor.value?.$editor?.getAction("editor.action.formatDocument")?.run();
}

function onUpdateModelValue() {
  emits("update:modelValue", code.value);
}

onMounted(() => {
  setTimeout(() => {
    code.value = props?.modelValue as string;
    formatEditor();
  }, 225);
});
</script>

<template>
  <Accordion
    :label="lang.toUpperCase()"
    :open-by-default="true"
    :content-is-full-width="true"
    class="text-title"
  >
    <template #toolbar>
      <Button
        type="inline"
        class="hover:bg-button-secondary-hover focus:bg-button-secondary-hover"
        :icon-only="true"
        icon="FormatAlignLeft"
        label="Format code"
        @click="formatEditor"
      />
    </template>
    <MonacoEditor
      ref="editor"
      v-model="code"
      @update:model-value="onUpdateModelValue"
      :lang="lang"
      :options="{
        theme: 'vs-dark',
        formatOnPaste: true,
        formatOnType: true,
        autoIndent: 'brackets',
        autoClosingBrackets: 'always',
        autoClosingComments: 'always',
        wordWrap: 'on',
        suggest: {
          insertMode: 'insert',
        },
      }"
      :style="{ width: '100%', height: '215px' }"
    />
  </Accordion>
</template>
