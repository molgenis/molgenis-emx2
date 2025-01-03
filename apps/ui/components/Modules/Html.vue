<template>
  <div>
    <div v-html="localContent.html"></div>
    <EditBlock v-if="editMode" type="Html" @action="$emit('action', $event)">
      <InputTextArea
        id="html input"
        :modelValue="localContent.html"
        @update:modelValue="save($event)"
        placeholder="Add your HTML here"
      ></InputTextArea>
    </EditBlock>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from "vue";

let props = withDefaults(
  defineProps<{
    content?: { html: string };
    editMode?: boolean;
  }>(),
  {
    editMode: false,
  }
);

const emit = defineEmits(["save", "action"]);

let localContent = ref(props.content);

function save(value) {
  localContent.value.html = value;
  emit("save", localContent.value);
}

watch(
  () => props.content,
  (newValue) => {
    localContent.value = newValue;
  }
);
</script>
