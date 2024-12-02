<template>
  <div>
    <component
      :is="modules[content?.type]"
      :content="content"
      @save="save($event)"
      :editMode="editMode"
      :page="page"
    ></component>
  </div>
</template>

<script setup lang="ts">
import Html from "./Html.vue";
import Header from "./Header.vue";
import Section from "./Section.vue";
import PieChart from "./PieChart.vue";
import { ref, watch } from "vue";

const modules = {
  Html: Html,
  Section: Section,
  Header: Header,
  PieChart: PieChart,
};
let props = withDefaults(
  defineProps<{
    content?: { type: string };
    editMode?: boolean;
    page: string;
  }>(),
  {
    editMode: false,
  }
);

const emit = defineEmits();

let localContent = ref(props.content);

function save(value) {
  localContent.value = value;
  emit("save", localContent.value);
}

watch(
  () => props.content,
  (newValue) => {
    localContent.value = newValue;
  }
);
</script>
