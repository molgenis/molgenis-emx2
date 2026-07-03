<script setup lang="ts">
// @ts-ignore
import { FileList } from "molgenis-viz";
import { computed } from "vue";

const props = defineProps<{
  user: string;
  labelValue?: string;
}>();

const filterArgument = computed(() => {
  let filter = 'filter: { tags: { equals: "private" }';
  if (props.labelValue) {
    filter += `, label: { equals: "${props.labelValue}" }`;
  }

  filter += " }";
  return filter;
});
</script>

<template>
  <FileList
    v-if="user && user !== 'anonymous'"
    table="Files"
    :filter="filterArgument"
    labelsColumn="name"
    fileColumn="file"
  />
</template>
