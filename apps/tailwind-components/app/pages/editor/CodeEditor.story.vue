<script setup lang="ts">
import { ref } from "vue";
import CodeEditor from "../../components/editor/CodeEditor.vue";

const languages = ["html", "css", "javascript"] as const;

const code = ref({
  html: '<h1 class="greeting">Hello from the code editor</h1>',
  css: ".greeting { color: rebeccapurple; }",
  javascript: 'console.log("hello from the code editor");',
});

const spec = `
## Features
- Monaco editor for the html, css and javascript of a developer page
- Syntax highlighting, autocompletion and bracket closing per language
- Format button, and format on paste and on type
- Emits every keystroke as update:modelValue

## Props
| Prop | Type | Default |
|------|------|---------|
| lang | "html" \\| "css" \\| "javascript" | "html" |
| modelValue | string | undefined |

## Test Checklist
- [ ] Each editor renders its code with syntax colours, not as plain text
- [ ] Typing in an editor updates the component output below it
- [ ] The format button reindents the code
- [ ] Autocompletion popup appears, which proves the language web workers started
- [ ] The browser console shows no "Could not create web worker(s)"
`;
</script>

<template>
  <Story title="CodeEditor" :spec="spec">
    <div class="grid gap-7.5 p-6">
      <div v-for="language in languages" :key="language" class="grid gap-2.5">
        <CodeEditor :lang="language" v-model="code[language]" />
        <StoryComponentOutput>{{ code[language] }}</StoryComponentOutput>
      </div>
    </div>
  </Story>
</template>
