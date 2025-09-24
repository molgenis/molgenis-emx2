<script lang="ts" setup>
import { ref, useTemplateRef, watchEffect } from "vue";
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

const code = defineModel<string>("");
const isExpanded = ref<boolean>(true);
const editor = useTemplateRef<InstanceType<typeof MonacoEditor>>("editor");

watchEffect(() => {
  if (editor.value?.$editor) {
    const action = editor.value.$editor?.getAction(
      "editor.action.formatDocument"
    );
    if (action) {
      action.run();
    }
  }
});

watchEffect(() => {
  if (props.modelValue) {
    code.value = props.modelValue;
  }
});
</script>

<template>
  <div :id="`${lang}-editor`" class="border">
    <div class="border pl-4 flex justify-start items-center">
      <button
        :id="`${lang}-editor-toggle`"
        class="w-full pl-4 py-4 text-button-text flex justify-start items-center"
        @click="isExpanded = !isExpanded"
        :aria-expanded="isExpanded"
        :aria-controls="`${lang}-editor-content`"
      >
        <BaseIcon name="CaretDown" :width="24" />
        <span>{{ lang }}</span>
      </button>
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
        @update:model-value="emits('update:modelValue', code as string)"
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
        :style="{ width: '100%', height: '350px' }"
      />
    </div>
  </div>
</template>
