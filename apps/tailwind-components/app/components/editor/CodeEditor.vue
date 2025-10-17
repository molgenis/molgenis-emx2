<script lang="ts" setup>
import { ref, useTemplateRef, onMounted } from "vue";
import Button from "../Button.vue";

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
  <div class="border border-input">
    <div
      class="flex justify-between items-center gap-5 text-title p-2.5 px-7.5"
    >
      <div>
        <span class="text-left capitalize font-bold text-clip">{{
          lang.toUpperCase()
        }}</span>
      </div>
      <Button
        type="inline"
        class="hover:bg-button-secondary-hover focus:bg-button-secondary-hover"
        :icon-only="true"
        icon="FormatAlignLeft"
        label="Format code"
        @click="formatEditor"
      />
    </div>
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
      :style="{ width: '100%', height: '235px' }"
    />
  </div>
</template>
