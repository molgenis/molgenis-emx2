<script lang="ts" setup>
import { ref, useTemplateRef, onMounted, watch } from "vue";

const props = withDefaults(
  defineProps<{
    lang: "html" | "css" | "javascript";
    modelValue?: string;
  }>(),
  {
    lang: "html",
  }
);

const isExpanded = ref<boolean>(true);
const code = ref<string>(
  "<div><h1>hello, world!</h1><p>test</p><p>test 1</p></div>"
);
const editor = useTemplateRef<HTMLDivElement>("editor");

const options = {
  automaticLayout: true,
  value: code.value,
  language: props.lang,
  theme: "vs-dark",
  formatOnPaste: true,
  formatOnType: true,
  autoIndent: "brackets",
  autoClosingBrackets: "always",
  wordWrap: "on",
  dimension: {
    height: 310,
    width: 100,
  },
  suggest: {
    insertMode: "insert",
  },
};

onMounted(() => {
  // monacoEditor.value = monaco.editor.create(editor.value as HTMLElement, {
  // });
});

// watch(() => monacoEditor.value, () => {
//   monacoEditor.value.getModel().onDidChangeContent(() => {
//     code.value = toRaw(monacoEditor.value).getValue()
//   })
// });
</script>

<template>
  {{ code }}
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
      <!-- <Button
        type="secondary"
        icon="UploadFile"
        :icon-only="true"
        label="Format code"
        size="small"
        class="mx-2"
        @click="formatEditor"
      /> -->
    </div>
    <div
      :id="`${lang}-editor-content`"
      :class="{
        block: isExpanded,
        hidden: !isExpanded,
      }"
    >
      <MonacoEditor
        v-model="code"
        :lang="lang"
        :options="{
          theme: 'vs-dark',
          dimension: {
            height: 350,
            width: 100,
          },
        }"
      />
    </div>
  </div>
</template>
