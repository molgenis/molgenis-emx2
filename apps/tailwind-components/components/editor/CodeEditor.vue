<script lang="ts" setup>
import { ref, useTemplateRef, onMounted } from "vue";
import { MonacoEditor } from "#components";

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
const isExpanded = ref<boolean>(true);
const editor = useTemplateRef<InstanceType<typeof MonacoEditor>>("editor");

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
  }, 125);
});
</script>

<template>
  <div :id="`${lang}-editor`" class="border">
    <div class="px-4 flex justify-start items-center">
      <button
        :id="`${lang}-editor-toggle`"
        class="w-full pl-4 py-4 text-button-text flex justify-start items-center"
        @click="isExpanded = !isExpanded"
        :aria-expanded="isExpanded"
        :aria-controls="`${lang}-editor-content`"
      >
        <BaseIcon name="CaretDown" :width="24" />
        <span>{{ lang.toUpperCase() }}</span>
      </button>
      <Button
        type="outline"
        size="small"
        :icon-only="true"
        icon="UploadFile"
        label="format document"
        @click="formatEditor"
      />
    </div>
    <div
      ref="container"
      :id="`${lang}-editor-content`"
      :class="{
        static: isExpanded,
        hidden: !isExpanded,
      }"
    >
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
        :style="{ width: '100%', height: '250px' }"
      />
    </div>
  </div>
</template>
