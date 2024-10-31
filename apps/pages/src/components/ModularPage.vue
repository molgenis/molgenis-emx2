<template>
  <Page>
    <Module
      v-for="(module, index) in localContent?.modules"
      @save="save($event, index)"
      :content="module"
      :editMode="editMode"
      :page="page"
    ></Module>
  </Page>
</template>

<script setup lang="ts">
import Module from "./Modules/Module.vue";
import { Page } from "molgenis-viz";
import { ref, watch } from "vue";

let props = withDefaults(
  defineProps<{
    content?: { modules: any[] };
    editMode?: boolean;
    page: string;
  }>(),
  {
    editMode: false,
  }
);

const emit = defineEmits();

let localContent = ref(props.content);

function save(value, index) {
  emit("save", localContent.value);
}

watch(
  () => props.content,
  (newValue) => {
    localContent.value = newValue;
  }
);
</script>
