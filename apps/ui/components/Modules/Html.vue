<template>
  <div>
    <div v-html="localContent.html"></div>
    <EditBlock v-if="editMode" type="Html">
      <InputText
        :modelValue="localContent.html"
        @update:modelValue="save($event)"
        placeholder="Add your HTML here"
      ></InputText>
    </EditBlock>
  </div>
</template>

<script setup lang="ts">
import EditBlock from "../EditBlock.vue";
import { InputText } from "molgenis-components";
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

const emit = defineEmits();

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
